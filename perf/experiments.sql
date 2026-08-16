-- ============================================================================
-- Before/after index experiments. Each section measures a candidate fix by
-- running the affected hot query, applying/dropping an index, and re-running.
-- EXPLAIN ANALYZE timings land in the captured output; RESULTS.md digests them.
-- ============================================================================

\set ON_ERROR_STOP on
\pset pager off

SELECT id AS hot_user FROM bench_user_ids WHERE idx = 1 \gset
SELECT id AS mid_user FROM bench_user_ids WHERE idx = 20000 \gset

-- ---------------------------------------------------------------------------
-- E1: participants(user_id) — the MatchRepository gap.
--     Entity declares NO index; PK (match_id, user_id) can't serve user_id.
-- ---------------------------------------------------------------------------
\echo '=== E1-BEFORE MatchRepository.findHistory (no participants.user_id index) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT m.* FROM matches m JOIN participants p ON p.match_id = m.id
WHERE p.user_id = :'hot_user' ORDER BY m.match_date DESC LIMIT 20;

\echo '=== E1-BEFORE MatchRepository.countTotalMatches ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM matches m JOIN participants p ON p.match_id = m.id
WHERE p.user_id = :'hot_user';

CREATE INDEX idx_participants_user ON participants (user_id, match_id);

\echo '=== E1-AFTER MatchRepository.findHistory (with idx_participants_user) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT m.* FROM matches m JOIN participants p ON p.match_id = m.id
WHERE p.user_id = :'hot_user' ORDER BY m.match_date DESC LIMIT 20;

\echo '=== E1-AFTER MatchRepository.countTotalMatches ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM matches m JOIN participants p ON p.match_id = m.id
WHERE p.user_id = :'hot_user';

-- ---------------------------------------------------------------------------
-- E2: friendships(receiver_id) — unique(requester_id, receiver_id) serves the
--     requester side only; receiver-side lookups (pending requests, badge)
--     have nothing.
-- ---------------------------------------------------------------------------
\echo '=== E2-BEFORE FriendshipRepository.findPendingRequests (receiver side) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM friendships f WHERE f.receiver_id = :'hot_user' AND f.status = 'PENDING';

CREATE INDEX idx_friendship_receiver_status ON friendships (receiver_id, status);

\echo '=== E2-AFTER FriendshipRepository.findPendingRequests ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM friendships f WHERE f.receiver_id = :'hot_user' AND f.status = 'PENDING';

-- ---------------------------------------------------------------------------
-- E3: the notification indexes added on this branch — quantify what prod
--     gains by dropping them (old state) and re-measuring, then restoring.
-- ---------------------------------------------------------------------------
DROP INDEX idx_notification_recipient_unread;
DROP INDEX idx_notification_recipient_created;

\echo '=== E3-BEFORE (branch indexes DROPPED) bell badge count, hot user ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM notifications n
WHERE n.recipient_id = :'hot_user' AND n.is_read = false;

\echo '=== E3-BEFORE feed page 0, hot user ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM notifications n WHERE n.recipient_id = :'hot_user'
ORDER BY n.created_at DESC LIMIT 20;

CREATE INDEX idx_notification_recipient_unread ON notifications (recipient_id, is_read);
CREATE INDEX idx_notification_recipient_created ON notifications (recipient_id, created_at);

\echo '=== E3-AFTER (branch indexes restored) bell badge count ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM notifications n
WHERE n.recipient_id = :'hot_user' AND n.is_read = false;

\echo '=== E3-AFTER feed page 0 ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM notifications n WHERE n.recipient_id = :'hot_user'
ORDER BY n.created_at DESC LIMIT 20;

-- ---------------------------------------------------------------------------
-- E4: bookings(user_id) — same quantification for the player-view index
--     added on this branch.
-- ---------------------------------------------------------------------------
DROP INDEX idx_booking_user_time;

\echo '=== E4-BEFORE (branch index DROPPED) getMyUpcoming, hot user ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM bookings b
WHERE b.user_id = :'hot_user' AND b.end_time >= now()
  AND b.status IN ('PENDING','CONFIRMED')
ORDER BY b.start_time ASC LIMIT 20;

CREATE INDEX idx_booking_user_time ON bookings (user_id, start_time);

\echo '=== E4-AFTER getMyUpcoming ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM bookings b
WHERE b.user_id = :'hot_user' AND b.end_time >= now()
  AND b.status IN ('PENDING','CONFIRMED')
ORDER BY b.start_time ASC LIMIT 20;

-- ---------------------------------------------------------------------------
-- E6: getMyUpcoming shape. (user_id, start_time) forces the planner to walk
--     history before reaching future rows for heavy users. Candidate:
--     (user_id, status, start_time) — the status filter is what's selective
--     once past CONFIRMED bookings get lazily flipped to COMPLETED.
-- ---------------------------------------------------------------------------
\echo '=== E6-BEFORE getMyUpcoming on (user_id, start_time) only ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM bookings b
WHERE b.user_id = :'hot_user' AND b.end_time >= now()
  AND b.status IN ('PENDING','CONFIRMED')
ORDER BY b.start_time ASC LIMIT 20;

CREATE INDEX idx_booking_user_status_time ON bookings (user_id, status, start_time);

\echo '=== E6-AFTER getMyUpcoming with (user_id, status, start_time) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM bookings b
WHERE b.user_id = :'hot_user' AND b.end_time >= now()
  AND b.status IN ('PENDING','CONFIRMED')
ORDER BY b.start_time ASC LIMIT 20;

DROP INDEX idx_booking_user_status_time;

-- ---------------------------------------------------------------------------
-- E5: partition-pruning taste test — is time partitioning worth it at this
--     scale? Compare the same conflict probe against a start_time-partitioned
--     copy of bookings (monthly ranges).
-- ---------------------------------------------------------------------------
CREATE TABLE bookings_part (LIKE bookings INCLUDING DEFAULTS)
PARTITION BY RANGE (start_time);

DO $$
DECLARE m date;
BEGIN
  FOR m IN SELECT generate_series(date_trunc('month', now() - interval '4 months'),
                                  date_trunc('month', now() + interval '4 months'),
                                  interval '1 month')::date
  LOOP
    EXECUTE format(
      'CREATE TABLE bookings_part_%s PARTITION OF bookings_part
       FOR VALUES FROM (%L) TO (%L)',
      to_char(m, 'YYYY_MM'), m, m + interval '1 month');
  END LOOP;
END $$;

INSERT INTO bookings_part SELECT * FROM bookings
WHERE start_time >= date_trunc('month', now() - interval '4 months')
  AND start_time <  date_trunc('month', now() + interval '5 months');

CREATE INDEX idx_part_court_time ON bookings_part (court_id, start_time, end_time);
VACUUM ANALYZE bookings_part;

SELECT id AS hot_court FROM bench_court_ids WHERE idx = 1 \gset

\echo '=== E5-PLAIN conflict probe on monolithic bookings ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM bookings b
WHERE b.court_id = :'hot_court'
  AND b.status IN ('PENDING','CONFIRMED')
  AND b.start_time < now() + interval '26 hours'
  AND b.end_time   > now() + interval '25 hours';

\echo '=== E5-PARTITIONED same probe on bookings_part ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM bookings_part b
WHERE b.court_id = :'hot_court'
  AND b.status IN ('PENDING','CONFIRMED')
  AND b.start_time < now() + interval '26 hours'
  AND b.end_time   > now() + interval '25 hours';

DROP TABLE bookings_part;
