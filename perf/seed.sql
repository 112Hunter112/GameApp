-- ============================================================================
-- Skewed seed data. Real traffic is never uniform: a few courts and a few
-- users dominate. Skew comes from floor(N * random()^3), a power-law-ish
-- draw where index 0 (the "hot" court/user) receives roughly 8-9% of ALL
-- events, the top 10 receive ~20%, and the tail is thin — the shape that
-- creates hot partitions in production.
--
-- Volumes (fits comfortably in a few GB):
--   users 40k, venues 400, courts 1600, bookings 1M, notifications 2M,
--   friendships 400k, matches 300k (participants 600k), refresh tokens 150k,
--   reset codes 30k, smart-fill offers 100k.
-- ============================================================================

\timing on
\set ON_ERROR_STOP on

-- Deterministic-ish UUIDs from integers so seeding is repeatable and FK
-- references never need lookups: md5 is fine for synthetic data.
CREATE OR REPLACE FUNCTION suuid(prefix text, n bigint) RETURNS uuid
  IMMUTABLE LANGUAGE sql AS $$ SELECT md5(prefix || ':' || n)::uuid $$;

-- --- users -------------------------------------------------------------------
INSERT INTO users (id, first_name, last_name, email, password, phone_number,
                   joining_date, role, updated_at, is_verified, username,
                   email_notifications_enabled, push_notifications_enabled)
SELECT suuid('user', n),
       'First' || n, 'Last' || n,
       'user' || n || '@example.com',
       '$2a$10$hashhashhashhashhashha',
       CASE WHEN n % 3 = 0 THEN '608' || lpad(n::text, 7, '0') END,
       now() - (random() * interval '400 days'),
       CASE WHEN n < 400 THEN 'VENUE_OWNER' ELSE 'USER' END,
       now(), true,
       CASE WHEN n % 2 = 0 THEN 'player' || n END,
       true, true
FROM generate_series(0, 39999) n;

-- --- sports / venues / courts --------------------------------------------------
INSERT INTO sports (id, sport_name, min_players, max_players, is_active, scoring_type)
SELECT suuid('sport', n), (ARRAY['Tennis','Pickleball','Badminton','Squash',
                                 'Basketball','Volleyball','Futsal','TableTennis'])[n+1],
       2, 10, true, 'SETS'
FROM generate_series(0, 7) n;

INSERT INTO venues (id, owner_id, name, address, is_active, created_at, opening_hours)
SELECT suuid('venue', n), suuid('user', n % 400),
       'Venue ' || n, n || ' Main St, Madison WI', true,
       now() - interval '300 days', '{"MONDAY":"08:00-22:00"}'::jsonb
FROM generate_series(0, 399) n;

INSERT INTO courts (id, venue_id, sports_id, court_number, hourly_rate,
                    is_indoor, surface_type, capacity, is_active, created_at)
SELECT suuid('court', n), suuid('venue', n / 4), suuid('sport', n % 8),
       'Court-' || (n % 4 + 1), 20.00, n % 2 = 0, 'Hard', 4, true,
       now() - interval '300 days'
FROM generate_series(0, 1599) n;

-- pgbench handle tables
INSERT INTO bench_court_ids SELECT n + 1, suuid('court', n) FROM generate_series(0, 1599) n;
INSERT INTO bench_user_ids  SELECT n + 1, suuid('user', n)  FROM generate_series(0, 39999) n;

-- --- bookings: 1M, court skew, ±90 days, realistic status mix ------------------
INSERT INTO bookings (id, user_id, court_id, start_time, end_time, status,
                      total_price, payment_status, payment_method, created_at)
SELECT suuid('booking', n),
       suuid('user', floor(40000 * pow(random(), 3))::int),
       suuid('court', floor(1600 * pow(random(), 3))::int),
       s.start_time,
       s.start_time + interval '1 hour' * (1 + (n % 2)),
       CASE
         WHEN r < 0.55 THEN 'COMPLETED'
         WHEN r < 0.80 THEN 'CONFIRMED'
         WHEN r < 0.88 THEN 'CANCELLED'
         WHEN r < 0.93 THEN 'PENDING'
         WHEN r < 0.97 THEN 'DECLINED'
         ELSE 'NO_SHOW'
       END,
       40.00, 'PENDING', 'CASH', now() - (random() * interval '90 days')
FROM (
  SELECT n, random() AS r,
         date_trunc('hour', now() + ((random() * 180 - 90) * interval '1 day'))
           + (6 + floor(random() * 16)::int) * interval '1 hour' AS start_time
  FROM generate_series(0, 999999) n
) s;

-- --- notifications: 2M, recipient skew, mostly read ----------------------------
INSERT INTO notifications (id, recipient_id, sender_id, type, reference_id,
                           message, is_read, created_at)
SELECT suuid('notif', n),
       suuid('user', floor(40000 * pow(random(), 3))::int),
       CASE WHEN n % 3 = 0 THEN suuid('user', (n * 7) % 40000) END,
       (ARRAY['BOOKING_CREATED','BOOKING_CONFIRMED','FRIEND_REQUEST','MATCH_INVITE',
              'MATCH_VERIFIED','SMART_FILL_OFFER','BOOKING_CANCELLED'])[1 + n % 7],
       suuid('ref', n),
       'Notification body ' || n,
       random() < 0.85,
       now() - (random() * interval '180 days')
FROM generate_series(0, 1999999) n;

-- --- friendships: 400k unique pairs -------------------------------------------
INSERT INTO friendships (id, requester_id, receiver_id, status, created_at)
SELECT suuid('friend', n),
       suuid('user', floor(40000 * pow(random(), 3))::int),
       suuid('user', floor(40000 * pow(random(), 2))::int),
       CASE WHEN random() < 0.8 THEN 'ACCEPTED'
            WHEN random() < 0.9 THEN 'PENDING' ELSE 'DECLINED' END,
       now() - (random() * interval '300 days')
FROM generate_series(0, 449999) n
WHERE suuid('user', (n * 13) % 40000) IS NOT NULL  -- keep the planner honest
ON CONFLICT (requester_id, receiver_id) DO NOTHING;
-- self-friendships are harmless noise for perf purposes

-- --- matches + participants: 300k matches, 2 players each ----------------------
INSERT INTO matches (id, created_by_user_id, source, verification_status, score,
                     match_date, is_private, status, ratings_applied, created_at)
SELECT suuid('match', n),
       suuid('user', floor(40000 * pow(random(), 3))::int),
       CASE WHEN n % 4 = 0 THEN 'MANUAL' ELSE 'APP_BOOKING' END,
       CASE WHEN random() < 0.8 THEN 'CONFIRMED' ELSE 'PENDING' END,
       CASE WHEN random() < 0.9 THEN '6-4, 6-3' END,
       now() - ((random() * 360 - 30) * interval '1 day'),
       false, 'OPEN', random() < 0.7, now() - (random() * interval '360 days')
FROM generate_series(0, 299999) n;

-- creator is participant 1; opponent skewed; dedupe the rare self-match
INSERT INTO participants (match_id, user_id, status, is_host, team_name)
SELECT m.id, m.created_by_user_id, 'ACCEPTED', true, 'TEAM_A' FROM matches m;

INSERT INTO participants (match_id, user_id, status, is_host, team_name)
SELECT suuid('match', n), suuid('user', floor(40000 * pow(random(), 3))::int),
       'ACCEPTED', false, 'TEAM_B'
FROM generate_series(0, 299999) n
ON CONFLICT (match_id, user_id) DO NOTHING;

-- --- refresh tokens / reset codes / offers -------------------------------------
INSERT INTO refresh_tokens (id, user_id, token_hash, expiry_date, revoked, created_at)
SELECT suuid('rt', n),
       suuid('user', floor(40000 * pow(random(), 2))::int),
       md5('rt-hash' || n),
       now() + interval '30 days' - (random() * interval '60 days'),
       random() < 0.4,
       now() - (random() * interval '60 days')
FROM generate_series(0, 149999) n;

INSERT INTO password_reset_codes (id, user_id, code_hash, expires_at, attempts, used, created_at)
SELECT suuid('prc', n),
       suuid('user', floor(40000 * pow(random(), 2))::int),
       md5('code' || n),
       now() + interval '15 minutes' - (random() * interval '30 days'),
       floor(random() * 3)::int,
       random() < 0.9,
       now() - (random() * interval '30 days')
FROM generate_series(0, 29999) n;

INSERT INTO smart_fill_offers (id, court_id, slot_start, user_id, sent_at)
SELECT suuid('sfo', n),
       suuid('court', floor(1600 * pow(random(), 3))::int),
       date_trunc('hour', now() + (random() * 14 * interval '1 day')),
       suuid('user', n % 40000),
       now() - (random() * interval '14 days')
FROM generate_series(0, 99999) n
ON CONFLICT (court_id, slot_start, user_id) DO NOTHING;

VACUUM ANALYZE;

-- Quick sanity + skew report
SELECT 'bookings' t, count(*) FROM bookings UNION ALL
SELECT 'notifications', count(*) FROM notifications UNION ALL
SELECT 'friendships', count(*) FROM friendships UNION ALL
SELECT 'matches', count(*) FROM matches UNION ALL
SELECT 'participants', count(*) FROM participants;

SELECT 'hot court share %' AS metric,
       round(100.0 * count(*) FILTER (WHERE court_id = suuid('court', 0)) / count(*), 1)
FROM bookings
UNION ALL
SELECT 'hot user notif share %',
       round(100.0 * count(*) FILTER (WHERE recipient_id = suuid('user', 0)) / count(*), 1)
FROM notifications;
