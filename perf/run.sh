#!/usr/bin/env bash
# ============================================================================
# DuoSport DB stress-test harness. Requires a reachable PostgreSQL 15/16 and
# pgbench. All state lives in the target database — safe to re-run.
#
#   PGHOST=/tmp PGPORT=5433 PGUSER=postgres DB=stresstest ./perf/run.sh
#
# Stages (each skippable): schema -> seed -> explains -> experiments ->
# contention (pgbench) -> pk study. Output accumulates in perf/out/.
# ============================================================================
set -euo pipefail
cd "$(dirname "$0")/.."

PGHOST="${PGHOST:-/tmp}"
PGPORT="${PGPORT:-5433}"
PGUSER="${PGUSER:-postgres}"
DB="${DB:-stresstest}"
PSQL="psql -h $PGHOST -p $PGPORT -U $PGUSER -d $DB -X"
BENCH="pgbench -h $PGHOST -p $PGPORT -U $PGUSER"
OUT=perf/out
mkdir -p "$OUT"

stage() { echo; echo "########## $1 ##########"; }

stage "1/6 schema"
$PSQL -f perf/schema.sql > "$OUT/schema.log" 2>&1

stage "2/6 seed (~4.5M rows, few minutes)"
$PSQL -f perf/seed.sql > "$OUT/seed.log" 2>&1
tail -12 "$OUT/seed.log"

stage "3/6 EXPLAIN ANALYZE every repository query"
$PSQL -f perf/explain-all.sql > "$OUT/explains.log" 2>&1
grep -E "^===|Execution Time" "$OUT/explains.log" | sed 's/^/  /'

stage "4/6 index before/after experiments"
$PSQL -f perf/experiments.sql > "$OUT/experiments.log" 2>&1
grep -E "^===|Execution Time" "$OUT/experiments.log" | sed 's/^/  /'

stage "5/6 contention sims (pgbench, 8 clients x 15s each)"
echo "--- hot court (all clients, ONE court row) ---"
$BENCH -n -c 8 -j 4 -T 15 -f perf/contention/book-hot-court.sql "$DB" \
  | tee "$OUT/bench-hot-court.log" | grep -E "tps|latency"
echo "--- spread courts (same tx, 1600 courts) ---"
$BENCH -n -c 8 -j 4 -T 15 -f perf/contention/book-spread-courts.sql "$DB" \
  | tee "$OUT/bench-spread.log" | grep -E "tps|latency"
echo "--- badge polling (16 clients, hot-user weighted) ---"
$BENCH -n -c 16 -j 4 -T 10 -f perf/contention/badge-poll.sql "$DB" \
  | tee "$OUT/bench-badge.log" | grep -E "tps|latency"

stage "6/6 primary-key strategy study"
$PSQL -f perf/pk-experiment.sql > "$OUT/pk.log" 2>&1
grep -E "^===|Time:|v4|v7ish|bigserial|correlation|pk_" "$OUT/pk.log" | head -30 | sed 's/^/  /'

echo
echo "Done. Full logs in $OUT/. Digest lives in perf/RESULTS.md."
