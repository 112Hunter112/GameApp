-- ============================================================================
-- EXPLAIN (ANALYZE, BUFFERS) for every repository query, translated from the
-- JPQL/derived/native definitions. Parameters target the HOT court/user so
-- results reflect worst-case (hot-partition) behavior, not the easy tail.
--
-- Query IDs (Qnn) are referenced from RESULTS.md.
-- Sections: booking flow, notifications, auth, friendships, matches, vendor
-- dashboard, misc.
-- ============================================================================

\set ON_ERROR_STOP on
\pset pager off

-- Hot handles
SELECT id AS hot_court FROM bench_court_ids WHERE idx = 1 \gset
SELECT id AS hot_user  FROM bench_user_ids  WHERE idx = 1 \gset
SELECT id AS mid_user  FROM bench_user_ids  WHERE idx = 20000 \gset
SELECT id AS hot_venue FROM venues ORDER BY id LIMIT 1 \gset
SELECT owner_id AS hot_owner FROM venues WHERE id = :'hot_venue' \gset

\echo '=== Q01 BookingRepository.countConflicts (hot court, prime-time window) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM bookings b
WHERE b.court_id = :'hot_court'
  AND b.status IN ('PENDING','CONFIRMED')
  AND b.start_time < now() + interval '26 hours'
  AND b.end_time   > now() + interval '25 hours';

\echo '=== Q02 BookingRepository.findActiveInWindow (hot court, one day) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM bookings b
WHERE b.court_id = :'hot_court'
  AND b.status IN ('PENDING','CONFIRMED')
  AND b.start_time < date_trunc('day', now() + interval '1 day') + interval '22 hours'
  AND b.end_time   > date_trunc('day', now() + interval '1 day') + interval '8 hours'
ORDER BY b.start_time;

\echo '=== Q03 CourtBlockRepository.countOverlapping (hot court) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM court_blocks b
WHERE b.court_id = :'hot_court'
  AND b.start_time < now() + interval '26 hours'
  AND b.end_time   > now() + interval '25 hours';

\echo '=== Q04 BookingRepository.getMyUpcoming (hot user) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM bookings b
WHERE b.user_id = :'hot_user'
  AND b.end_time >= now()
  AND b.status IN ('PENDING','CONFIRMED')
ORDER BY b.start_time ASC
LIMIT 20;

\echo '=== Q05 BookingRepository.findPastForUser (hot user) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM bookings b
WHERE b.user_id = :'hot_user'
  AND (b.end_time < now() OR b.status IN ('CANCELLED','DECLINED','COMPLETED'))
ORDER BY b.start_time DESC
LIMIT 20;

\echo '=== Q06 BookingRepository.findForOwner (owner dashboard, no filters) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT b.* FROM bookings b
JOIN courts c ON c.id = b.court_id
JOIN venues v ON v.id = c.venue_id
WHERE v.owner_id = :'hot_owner'
ORDER BY b.start_time ASC
LIMIT 20;

\echo '=== Q07 BookingRepository.countPendingForOwner (badge) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM bookings b
JOIN courts c ON c.id = b.court_id
JOIN venues v ON v.id = c.venue_id
WHERE v.owner_id = :'hot_owner'
  AND b.status = 'PENDING'
  AND b.start_time > now();

\echo '=== Q08 NotificationRepository.countByRecipient_IdAndIsReadFalse (bell badge, hot user) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM notifications n
WHERE n.recipient_id = :'hot_user' AND n.is_read = false;

\echo '=== Q09 NotificationRepository.findByRecipient_IdOrderByCreatedAtDesc page 0 (hot user) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM notifications n
WHERE n.recipient_id = :'hot_user'
ORDER BY n.created_at DESC
LIMIT 20 OFFSET 0;

\echo '=== Q10 same feed, page 500 (deep offset pagination cost) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM notifications n
WHERE n.recipient_id = :'hot_user'
ORDER BY n.created_at DESC
LIMIT 20 OFFSET 10000;

\echo '=== Q11 NotificationRepository.markAllReadForUser (hot user) ==='
BEGIN;
EXPLAIN (ANALYZE, BUFFERS)
UPDATE notifications SET is_read = true
WHERE recipient_id = :'hot_user' AND is_read = false;
ROLLBACK;

\echo '=== Q12 UserRepository.findByEmail (login path) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM users WHERE email = 'user20000@example.com';

\echo '=== Q13 RefreshTokenRepository.findByTokenHash ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM refresh_tokens WHERE token_hash = md5('rt-hash99999');

\echo '=== Q14 RefreshTokenRepository.revokeAllForUser ==='
BEGIN;
EXPLAIN (ANALYZE, BUFFERS)
UPDATE refresh_tokens SET revoked = true
WHERE user_id = :'hot_user' AND revoked = false;
ROLLBACK;

\echo '=== Q15 RefreshTokenRepository.deleteExpired (maintenance) ==='
BEGIN;
EXPLAIN (ANALYZE, BUFFERS)
DELETE FROM refresh_tokens WHERE expiry_date < now() - interval '10 days';
ROLLBACK;

\echo '=== Q16 PasswordResetCodeRepository.findFirstByUserIdAndUsedFalse... ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM password_reset_codes
WHERE user_id = :'hot_user' AND used = false
ORDER BY created_at DESC LIMIT 1;

\echo '=== Q17 PasswordResetCodeRepository.countByUserIdAndCreatedAtAfter (rate limit) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM password_reset_codes
WHERE user_id = :'hot_user' AND created_at > now() - interval '1 hour';

\echo '=== Q18 FriendshipRepository.findAllFriends (hot user, both directions) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM friendships f
WHERE (f.requester_id = :'hot_user' OR f.receiver_id = :'hot_user')
  AND f.status = 'ACCEPTED';

\echo '=== Q19 FriendshipRepository.findPendingRequests (receiver side only) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM friendships f
WHERE f.receiver_id = :'hot_user' AND f.status = 'PENDING';

\echo '=== Q20 FriendshipRepository.areFriends (pair probe) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) > 0 FROM friendships f
WHERE ((f.requester_id = :'hot_user' AND f.receiver_id = :'mid_user')
    OR (f.requester_id = :'mid_user' AND f.receiver_id = :'hot_user'))
  AND f.status = 'ACCEPTED';

\echo '=== Q21 FriendshipRepository.countPendingRequests (badge) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM friendships f
WHERE f.receiver_id = :'hot_user' AND f.status = 'PENDING';

\echo '=== Q22 MatchRepository.findHistory (hot user, paged) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT m.* FROM matches m
JOIN participants p ON p.match_id = m.id
WHERE p.user_id = :'hot_user'
ORDER BY m.match_date DESC
LIMIT 20;

\echo '=== Q23 MatchRepository.countTotalMatches (hot user) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM matches m
JOIN participants p ON p.match_id = m.id
WHERE p.user_id = :'hot_user';

\echo '=== Q24 MatchRepository.findHeadToHead (two users) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT m.* FROM matches m
JOIN participants p1 ON p1.match_id = m.id
JOIN participants p2 ON p2.match_id = m.id
WHERE p1.user_id = :'hot_user' AND p2.user_id = :'mid_user';

\echo '=== Q25 MatchRepository.findUpcomingMatches (hot user) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT m.* FROM matches m
JOIN participants p ON p.match_id = m.id
WHERE p.user_id = :'hot_user' AND m.match_date > now()
ORDER BY m.match_date ASC;

\echo '=== Q26 MatchRepository.findFriendsRecentActivity (50 friend ids) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT DISTINCT m.* FROM matches m
JOIN participants p ON p.match_id = m.id
WHERE p.user_id IN (SELECT id FROM bench_user_ids WHERE idx BETWEEN 2 AND 51)
ORDER BY m.match_date DESC
LIMIT 20;

\echo '=== Q27 UserRepository.searchUsers (ILIKE name search) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM users u
WHERE u.first_name ILIKE '%First123%'
   OR u.last_name ILIKE '%First123%'
   OR u.username ILIKE '%First123%'
LIMIT 20;

\echo '=== Q28 VendorDashboard sumRevenue (hot venue, 30-day window) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT COALESCE(SUM(b.total_price), 0) FROM bookings b
JOIN courts c ON c.id = b.court_id
WHERE c.venue_id = :'hot_venue'
  AND b.status IN ('CONFIRMED','COMPLETED')
  AND b.start_time >= now() - interval '30 days' AND b.start_time < now();

\echo '=== Q29 VendorDashboard courtUtilization (native, hot venue) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.id, c.court_number, c.hourly_rate, COUNT(b.id),
       COALESCE(SUM(EXTRACT(EPOCH FROM (b.end_time - b.start_time)) / 3600.0), 0)
FROM courts c
LEFT JOIN bookings b ON b.court_id = c.id
  AND b.status IN ('CONFIRMED','COMPLETED')
  AND b.start_time >= now() - interval '30 days' AND b.start_time < now()
WHERE c.venue_id = :'hot_venue'
GROUP BY c.id, c.court_number, c.hourly_rate;

\echo '=== Q30 VendorDashboard occupancyHeatmap (native, hot venue, 90 days) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT EXTRACT(ISODOW FROM b.start_time)::int, EXTRACT(HOUR FROM b.start_time)::int, COUNT(*)
FROM bookings b
JOIN courts c ON c.id = b.court_id
WHERE c.venue_id = :'hot_venue'
  AND b.status IN ('CONFIRMED','COMPLETED')
  AND b.start_time >= now() - interval '90 days' AND b.start_time < now()
GROUP BY 1, 2 ORDER BY 1, 2;

\echo '=== Q31 PlayerReliability countStatusesForUsers (20 users) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT b.user_id, b.status, count(*) FROM bookings b
WHERE b.user_id IN (SELECT id FROM bench_user_ids WHERE idx BETWEEN 1 AND 20)
GROUP BY b.user_id, b.status;

\echo '=== Q32 SmartFillOfferRepository.findNotifiedUserIds ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT o.user_id FROM smart_fill_offers o
WHERE o.court_id = :'hot_court'
  AND o.slot_start = date_trunc('hour', now() + interval '1 day');

\echo '=== Q33 SmartFillOfferRepository.countDistinctSlotsSince (anti-spam) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(DISTINCT o.slot_start) FROM smart_fill_offers o
WHERE o.court_id = :'hot_court' AND o.sent_at >= now() - interval '7 days';

\echo '=== Q34 CourtRepository.findByVenue_Id ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM courts WHERE venue_id = :'hot_venue';

\echo '=== Q35 CourtBlockRepository.findForOwner (host calendar, one day) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT b.* FROM court_blocks b
JOIN courts c ON c.id = b.court_id
JOIN venues v ON v.id = c.venue_id
WHERE v.owner_id = :'hot_owner'
  AND b.start_time < date_trunc('day', now()) + interval '1 day'
  AND b.end_time > date_trunc('day', now())
ORDER BY b.start_time;

\echo '=== Q36 UserPreferenceRepository.findByUserId ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM user_preferences WHERE user_id = :'hot_user';

\echo '=== Q37 BookingRepository schedule view (venue, window, paged) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT b.* FROM bookings b
JOIN courts c ON c.id = b.court_id
WHERE c.venue_id = :'hot_venue'
  AND b.start_time BETWEEN now() AND now() + interval '7 days'
ORDER BY b.start_time ASC
LIMIT 50;

\echo '=== Q38 FeedShareRepository feed page (viewer with 50 friends incl. hot sharer) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM feed_shares
WHERE user_id IN (SELECT id FROM bench_user_ids WHERE idx BETWEEN 1 AND 51)
ORDER BY created_at DESC
LIMIT 20;

\echo '=== Q39 FeedShareRepository feed page (typical viewer, 15 friends, no heavy sharers) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM feed_shares
WHERE user_id IN (SELECT id FROM bench_user_ids WHERE idx BETWEEN 30000 AND 30014)
ORDER BY created_at DESC
LIMIT 20;

\echo '=== Q40 FeedShare target cleanup lookup (match deleted) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM feed_shares
WHERE target_type = 'MATCH' AND target_id = (SELECT suuid('match', 12345));

\echo '=== Q41 FeedShare duplicate guard (exists probe) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT 1 FROM feed_shares
WHERE user_id = :'hot_user' AND target_type = 'MATCH'
  AND target_id = (SELECT suuid('match', 777)) LIMIT 1;
