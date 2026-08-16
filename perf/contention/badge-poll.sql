-- pgbench script: the bell-badge unread count, weighted toward hot users the
-- way real polling is (every open app polls; heavy users have big rows).
\set r random(1, 100)
\set uidx CASE WHEN :r <= 40 THEN 1 ELSE random(1, 40000) END
SELECT count(*) FROM notifications n
WHERE n.recipient_id = (SELECT id FROM bench_user_ids WHERE idx = :uidx)
  AND n.is_read = false;
