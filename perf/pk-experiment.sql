-- ============================================================================
-- Primary-key strategy study. The app uses GenerationType.UUID => random
-- UUIDv4. Random keys spray inserts across the whole PK index (page splits,
-- cold-cache writes, WAL amplification); time-ordered keys append.
--
-- Contenders, 500k inserts each into an identical table:
--   A) uuid v4 (today's behavior)
--   B) uuid v7-style (48-bit ms timestamp prefix + random) — what Hibernate
--      6.5+ offers via @UuidGenerator(style = Style.VERSION_7)
--   C) bigserial (baseline; not shard-friendly, shown for scale)
--
-- Metrics: wall time, PK index size, index correlation to insert order.
-- ============================================================================

\timing on
\pset pager off

-- v7-style generator: millisecond timestamp in the top 48 bits, random tail.
CREATE OR REPLACE FUNCTION uuid_v7ish() RETURNS uuid LANGUAGE sql VOLATILE AS $$
  SELECT encode(
    set_bit(set_bit(
      overlay(uuid_send(gen_random_uuid())
              placing substring(int8send((extract(epoch from clock_timestamp())*1000)::bigint) from 3)
              from 1 for 6),
    52, 1), 53, 1), 'hex')::uuid
$$;

CREATE TABLE pk_v4     (id uuid PRIMARY KEY,   payload text);
CREATE TABLE pk_v7     (id uuid PRIMARY KEY,   payload text);
CREATE TABLE pk_serial (id bigserial PRIMARY KEY, payload text);

\echo '=== PK-A: 500k inserts, random UUIDv4 (current app behavior) ==='
INSERT INTO pk_v4 SELECT gen_random_uuid(), 'x' FROM generate_series(1, 500000);

\echo '=== PK-B: 500k inserts, time-ordered UUIDv7-style ==='
INSERT INTO pk_v7 SELECT uuid_v7ish(), 'x' FROM generate_series(1, 500000);

\echo '=== PK-C: 500k inserts, bigserial ==='
INSERT INTO pk_serial (payload) SELECT 'x' FROM generate_series(1, 500000);

VACUUM ANALYZE pk_v4; VACUUM ANALYZE pk_v7; VACUUM ANALYZE pk_serial;

\echo '=== PK index sizes (smaller = denser pages = better cache behavior) ==='
SELECT 'v4' AS strategy, pg_size_pretty(pg_relation_size('pk_v4_pkey')) AS pk_index
UNION ALL SELECT 'v7ish', pg_size_pretty(pg_relation_size('pk_v7_pkey'))
UNION ALL SELECT 'bigserial', pg_size_pretty(pg_relation_size('pk_serial_pkey'));

\echo '=== Correlation between physical order and PK order (1.0 = append-only) ==='
SELECT tablename, attname, correlation FROM pg_stats
WHERE tablename IN ('pk_v4','pk_v7','pk_serial') AND attname = 'id';

DROP TABLE pk_v4, pk_v7, pk_serial;
