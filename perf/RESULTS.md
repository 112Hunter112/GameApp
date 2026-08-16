# DB Stress-Test Results — 2026-08-16

Environment: PostgreSQL 16.13, single node, defaults except
`shared_buffers=512MB, work_mem=32MB`. Data seeded by `seed.sql` with
production-shaped skew:

| Table | Rows | Skew |
|---|---|---|
| bookings | 1,000,000 | hot court holds **8.5%** of all bookings |
| notifications | 2,000,000 | hot user holds ~3% (58k rows) |
| participants | 600,000 | power-law per user |
| friendships | 445,000 | power-law |
| matches | 300,000 | — |
| users / courts | 40,000 / 1,600 | — |

Caveats: `venues.location` is stubbed (no PostGIS here), so the two spatial
queries are assessed analytically below. Timings are hot-user/hot-court
worst cases unless stated. Reproduce with `perf/run.sh`.

## 1. Verdict summary

**Healthy — no action:** the entire booking core. Conflict check on the
hottest court: **2.1ms**. Availability grid: 3.3ms. Owner dashboard, revenue,
utilization, heatmap aggregates: all ≤5ms at 1M bookings. Auth lookups
(email, token hash), reset codes, smart-fill: ≤0.1ms.

**Fixed on this branch (measured before → after):**

| Query | Before | After | Fix |
|---|---|---|---|
| Bell badge unread count (polled!) | 123.8ms | 1.3ms | `idx_notification_recipient_unread` |
| Notification feed page 0 | 91.2ms | 0.1ms | `idx_notification_recipient_created` |
| My upcoming bookings | 80.7ms | ~20ms | `idx_booking_user_time` |
| Match history, typical user | 26.3ms | **0.2ms** | `idx_participants_user` (new) |
| Pending friend requests / badge | 23.9ms | 0.7ms | `idx_friendship_receiver_status` (new) |

The last two were found by this stress test: **every** `MatchRepository`
per-user query (~30 of them: history, stats, head-to-head, streaks, friends
activity) joined `participants` on `user_id` with nothing to serve it — the
composite PK `(match_id, user_id)` can't — so every call scanned 600k rows.
Friendships had the mirror problem: the unique `(requester_id, receiver_id)`
constraint indexes the requester side only.

**Documented, deliberately not changed (see §4):** deep feed pagination,
reliability aggregate over unbounded history, ILIKE searches, badge-count
caching, spatial indexes.

## 2. Contention simulations (pgbench)

**Hot-court booking race** — the full booking transaction (court row lock →
overlap checks → insert booking + notification), 8 concurrent clients:

| Scenario | TPS | avg latency |
|---|---|---|
| All clients on ONE court (worst hot partition) | **316/s** | 25.3ms |
| Same transaction spread over 1,600 courts | 3,182/s | 2.5ms |

Reading: per-court serialization costs ~10× throughput on that court — and it
does not matter. A court sells ~32 slots/day; 316 bookings/sec on a single
court is four orders of magnitude above real demand. **The pessimistic-lock
design is correct for this product at any plausible scale.** Zero failed
transactions, zero double-bookings in both runs.

**Badge polling storm** — 16 clients, 40% of polls hitting the hot user:
~596 polls/sec sustained, 27ms average (the hot user's count walks ~10k index
entries per poll). Fine today; §4 has the cheap fix when polling load grows.

## 3. Primary keys, sharding, partitioning

**UUIDv4 (current) vs time-ordered vs bigserial** — 500k inserts each:

| Strategy | Insert time | PK index size | physical correlation |
|---|---|---|---|
| UUIDv4 (today) | 2.67s | 19MB | −0.002 (random spray) |
| UUIDv7-style | 2.47s (−8%) | 20MB | 0.99999 (append-only) |
| bigserial | 1.29s | 11MB | 1.0 |

Verdict: **UUIDv4 is fine at current scale** — the penalty is ~8% on inserts
plus poor cache locality that only bites when an index no longer fits in RAM.
When `notifications`/`bookings` head toward tens of millions of rows, move new
rows to time-ordered UUIDs (Hibernate 6.5+: `@UuidGenerator(style = VERSION_7)`)
— keep UUIDs either way, they stay shard-friendly (no ID coordinator needed),
which bigserial is not.

**Partitioning bookings by month:** conflict probe 1.5ms plain vs 1.2ms
partitioned at 1M rows — **not worth the operational complexity now**. The
`(court_id, start_time, end_time)` index already prunes better than partitions
would. Revisit only past ~50M rows or when archiving old bookings matters.

**Sharding readiness (analysis):** write paths shard cleanly — bookings by
court/venue (the court-row lock stays shard-local), notifications by
recipient, matches by participant with duplication. UUID PKs mean no
cross-shard ID coordination. The scatter-gather pain would be the owner
dashboard (`booking → court → venue → owner` joins) and `findFriendsRecentActivity`.
Single Postgres + read replica will carry this product a very long way before
any of that is worth building.

## 4. Known costs, deliberately deferred

1. **Deep feed pagination:** page 0 = 0.1ms, offset 10,000 = 61.5ms. OFFSET
   walks and discards. Nobody scrolls 500 pages, so ship it — but when the
   mobile feed moves to infinite scroll, switch to keyset pagination
   (`WHERE (created_at, id) < (:last_seen, :last_id) ORDER BY ... LIMIT 20`).
2. **Reliability aggregate** (`countStatusesForUsers`, 20 users incl. hot):
   242ms because it aggregates each player's entire booking history. Bound it
   in code to a rolling window (e.g. 12 months) — reliability from 2 years ago
   is stale signal anyway.
3. **Badge counts under heavy polling:** cap the display at `99+` and query
   with `LIMIT 100` under it, or cache counts in Redis keyed by user, bumped
   on notification insert. Only needed when polling traffic grows.
4. **`searchUsers` / venue ILIKE `'%q%'`:** sequential scans by construction.
   17ms at 40k users — fine. At real scale add `pg_trgm` GIN indexes
   (`CREATE EXTENSION pg_trgm`) via an init script; Hibernate cannot declare
   them.
5. **Spatial queries (assessed analytically — PostGIS not available here):**
   `findVenuesNearby`/`discoverVenues` cast `location::geography` inside
   `ST_DWithin`. A plain GiST index on the geometry column **cannot serve
   that cast** — the index must be on the expression:
   `CREATE INDEX idx_venues_location_geog ON venues USING gist ((location::geography));`
   Hibernate can't declare GiST/expression indexes, so this belongs in
   `init-postgis.sql`/a migration. At 400 venues a full scan is ~instant, so
   this is a scale item, not a today item.
6. **Unbounded `List<>` queries** in `MatchRepository`/`CourtRepository`
   (e.g. `findByCreatedByUser`, `findByIsActiveTrue`) return every matching
   row with no page bound — add `Pageable` when any of them can exceed a few
   hundred rows.
7. **Append-only tables without cleanup:** `notifications` and
   `password_reset_codes` never shrink. Add a scheduled purge (e.g.
   notifications read > 6 months, used/expired codes > 30 days) — mirrors the
   existing `RefreshTokenRepository.deleteExpired`.

## 5. How to re-run

```bash
# any Postgres 15/16 with pgbench; PostGIS optional (spatial is stubbed)
createdb stresstest
PGHOST=localhost PGPORT=5432 PGUSER=postgres DB=stresstest ./perf/run.sh
```

Raw logs land in `perf/out/` (gitignored); this file is the curated digest.
