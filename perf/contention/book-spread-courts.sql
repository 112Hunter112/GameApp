-- pgbench script: same booking transaction, but clients spread across all
-- 1600 courts — the "no hot partition" control group. The TPS delta vs
-- book-hot-court.sql is the pure cost of court-row serialization.
\set cidx random(1, 1600)
\set slot random(0, 31)
\set uid random(1, 40000)
BEGIN;
SELECT id FROM courts WHERE id = (SELECT id FROM bench_court_ids WHERE idx = :cidx) FOR UPDATE;
INSERT INTO bookings (id, user_id, court_id, start_time, end_time, status, total_price, payment_status, payment_method, created_at)
SELECT gen_random_uuid(),
       (SELECT id FROM bench_user_ids WHERE idx = :uid),
       (SELECT id FROM bench_court_ids WHERE idx = :cidx),
       date_trunc('day', now() + interval '30 day') + (:slot * interval '30 minutes') + interval '6 hours',
       date_trunc('day', now() + interval '30 day') + (:slot * interval '30 minutes') + interval '7 hours',
       'CONFIRMED', 40.00, 'PENDING', 'CASH', now()
WHERE NOT EXISTS (
  SELECT 1 FROM bookings b
  WHERE b.court_id = (SELECT id FROM bench_court_ids WHERE idx = :cidx)
    AND b.status IN ('PENDING','CONFIRMED')
    AND b.start_time < date_trunc('day', now() + interval '30 day') + (:slot * interval '30 minutes') + interval '7 hours'
    AND b.end_time   > date_trunc('day', now() + interval '30 day') + (:slot * interval '30 minutes') + interval '6 hours'
) AND NOT EXISTS (
  SELECT 1 FROM court_blocks cb
  WHERE cb.court_id = (SELECT id FROM bench_court_ids WHERE idx = :cidx)
    AND cb.start_time < date_trunc('day', now() + interval '30 day') + (:slot * interval '30 minutes') + interval '7 hours'
    AND cb.end_time   > date_trunc('day', now() + interval '30 day') + (:slot * interval '30 minutes') + interval '6 hours'
);
INSERT INTO notifications (id, recipient_id, sender_id, type, reference_id, message, is_read, created_at)
VALUES (gen_random_uuid(),
        (SELECT id FROM bench_user_ids WHERE idx = 1),
        (SELECT id FROM bench_user_ids WHERE idx = :uid),
        'BOOKING_CREATED', gen_random_uuid(), 'bench booking', false, now());
COMMIT;
