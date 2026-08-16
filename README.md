# DuoSport Backend (GameApp)

A Spring Boot backend that fuses **court booking**, **social matchmaking**,
**competitive match tracking (per-sport Elo + reliability scores)**, and a
**venue-owner operations suite** — one codebase, two experiences separated by
role.

- **Players** discover nearby venues (PostGIS geo-search), book courts by the
  hour, log and verify match results against friends, build Elo ratings,
  showcase awards, and receive Smart Fill offers when a slot opens near them.
- **Venue owners** upgrade in-app to host mode: approve or decline booking
  requests, set per-venue booking policies, block courts for maintenance or
  leagues, and run an analytics dashboard (revenue, occupancy heatmap,
  player reliability, no-show tracking, CSV export).

Deep-dive documentation lives in [`PROJECT_DESCRIPTION.md`](PROJECT_DESCRIPTION.md);
this README covers what the system is and how to build, test, and ship it.

## Technology stack

| Layer | Technology |
|---|---|
| Language / framework | Java 17, Spring Boot 3.4 (Web, Data JPA, Security, Validation, Mail, Actuator) |
| Database | PostgreSQL 15 + PostGIS (geospatial venue search), Hibernate 6 + hibernate-spatial |
| Cache / infra deps | Redis (spring-data-redis + Redisson starter on the classpath; not yet load-bearing in app logic) |
| Auth | Stateless JWT access tokens + rotating hashed refresh tokens, Google Sign-In verification, HaveIBeenPwned k-anonymity breach screening |
| API docs | springdoc-openapi (Swagger UI at `/swagger-ui.html` when running) |
| Tests | JUnit 5, Mockito, AssertJ, spring-security-test, Testcontainers (PostGIS) |
| Packaging / runtime | Docker multi-stage build (non-root), Docker Compose, Caddy TLS ingress |
| CI/CD | GitHub Actions — PR test gate + staged beta → prod delivery (GHCR images) |

## Architecture

Layered monolith: `Controller → Service → Repository`, DTOs at every boundary
(entities never reach the wire), role checks via Spring Security plus
per-resource ownership checks in services (IDOR guards).

Two data-layer decisions worth knowing before touching booking code:

- **Double-booking prevention** is a pessimistic lock on the court row
  (`CourtRepository#findByIdForUpdate`) held across the overlap checks for
  bookings *and* court blocks. Overlap semantics are half-open:
  `s1 < e2 AND e1 > s2` — back-to-back bookings share a boundary legally.
- **The schema is Hibernate-managed** (`ddl-auto: update`): the
  `@Index`/`@UniqueConstraint` annotations on entities *are* the production
  indexes. `SchemaHotPathGuardTest` fails the build if a hot-path index
  annotation disappears.

## Running locally

### Docker Compose (recommended)

```bash
docker compose up --build          # app + PostGIS + Redis (+ Jenkins, legacy)
```

App: `http://localhost:8080` — Swagger UI: `http://localhost:8080/swagger-ui.html`

### Maven (native debugging)

Requires local PostgreSQL (with PostGIS) and Redis. Configuration comes from
environment variables (defaults in parentheses):

| Variable | Purpose |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` (`localhost`/`5432`/`sportsapp`) | Postgres connection |
| `DB_USER` / `DB_PASSWORD` (`postgres`/`password`) | Postgres credentials |
| `REDIS_HOST` / `REDIS_PORT` (`localhost`/`6379`) | Redis |
| `JWT_SECRET` (**required**, ≥32 bytes Base64) | JWT signing key |
| `EMAIL_USERNAME` / `EMAIL_PASSWORD` (**required**) | SMTP for reset codes & verification |
| `APP_CORS_ALLOWED_ORIGINS` (localhost dev origins) | CORS allowlist — never `*` |
| `GOOGLE_OAUTH_CLIENT_IDS` (empty = disabled) | Google Sign-In audiences |
| `PWNED_CHECK_ENABLED` (`true`) | HaveIBeenPwned breach screening |

```bash
bash mvnw spring-boot:run
```

## Testing

The suite is split by speed; Surefire and Failsafe run different classes:

```bash
bash mvnw test      # *Test  — unit tests, no Docker, ~150 tests, seconds
bash mvnw verify    # + *IT  — Testcontainers integration tests (needs Docker)
```

What the integration tests cover, beyond the usual: booking **concurrency**
(N threads racing for one slot must produce exactly one booking —
`BookingConflictIT`), overlap boundary semantics, bulk-update scoping
(`markAllReadForUser`, `invalidateAllForUser`), auth flow end-to-end through
the MVC stack, and PostGIS distance queries.

A repo-local Claude Code subagent (`.claude/agents/db-test-engineer.md`)
encodes the DB-testing conventions — locking rules, IT boilerplate, seeding
recipes — for AI-assisted test work.

### Performance / stress harness

`perf/` contains a reusable stress harness for the data layer: a faithful
schema, skewed seed data (hot courts / hot users), `EXPLAIN ANALYZE` sweeps of
every repository query, pgbench contention simulations (hot-court booking
races, badge-polling storms), and a primary-key strategy study. See
[`perf/RESULTS.md`](perf/RESULTS.md) for the latest findings and
`perf/run.sh` to reproduce them against any Postgres.

## CI/CD

Two GitHub Actions workflows:

- **`ci.yml`** — pull-request gate: full `mvn verify` (unit + Testcontainers
  integration tests) on every PR, plus a manual/weekly OWASP dependency scan.
- **`deploy.yml`** — delivery on push to `main`:
  `verify` gate → build one Docker image → push to GHCR (`sha-…` + `:beta`) →
  auto-deploy to **beta** with an on-box `/actuator/health` gate →
  **production** behind a required-reviewer approval, which promotes the
  *same image digest* beta ran (never a rebuild), floats `:prod`/`:latest`,
  and rolls back automatically if the health check fails.

Deployment target is a Docker Compose stack behind Caddy (TLS) with UFW as
the network enforcement layer; `docker-compose.deploy.yml` switches the app
service from build-on-server to the pinned registry image.

## Repository map

| Path | What's there |
|---|---|
| `src/main/java/...` | controllers, services, repositories, entities, security, validation |
| `src/test/java/...` | unit tests (`*Test`) and Testcontainers ITs (`*IT`) |
| `perf/` | DB stress-test harness + results |
| `.github/workflows/` | CI and staged deploy pipelines |
| `PROJECT_DESCRIPTION.md` | full feature-by-feature product/technical description |
| `ApiFlow.md`, `MODEL_OVERVIEW.md`, `Booking.md` | API flows, data model, booking design notes |
| `Project_Explained_Backend.md`, `NotesAndThoughts.md` | architecture explainers / working notes |
| `SECURITY_CHANGES_REPORT.pdf`, `owasp-suppressions.xml` | security review artifacts |
| `Dockerfile`, `docker-compose*.yml`, `Caddyfile` | build and deployment stack |

## Project status

Actively developed two-person project. The booking engine, auth stack,
social/match features, and vendor dashboard are implemented and tested; the
legacy Jenkins flow is superseded by the GitHub Actions pipelines and slated
for removal. A large fork merge (KeshavAditya/GameApp) is planned — the test
suite and schema guards on this branch exist partly to make that rebase safe.
