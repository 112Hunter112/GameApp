# DuoSport — Resume & Technical Summary

A production-grade backend for a sports venue-booking + social matchmaking
platform. Use the bullets below directly on a resume; the deeper sections are
interview prep.

---

## ONE-LINE PROJECT DESCRIPTION

> DuoSport — a location-aware sports platform combining hourly court booking,
> social matchmaking, Elo skill rating, and a venue-owner operations
> dashboard, built on Spring Boot + PostgreSQL/PostGIS with Redis-backed
> distributed locking.

---

## TECH STACK (list these)

**Language / Framework:** Java 17, Spring Boot 3.4, Spring Security, Spring
Data JPA / Hibernate 6
**Database:** PostgreSQL 15 + **PostGIS** (geospatial), Hibernate Spatial,
JTS Topology Suite
**Caching / Concurrency:** Redis, **Redisson** (distributed locks)
**Auth:** JWT (access + rotating refresh tokens), BCrypt, Google OAuth2
(ID-token verification)
**Testing:** JUnit 5, Mockito, AssertJ, **Testcontainers** (real PostGIS in
integration tests)
**DevOps:** Docker, Docker Compose, **GitHub Actions CI** (unit + integration
+ OWASP dependency scan), Maven
**Other:** OpenAPI/Swagger, Caddy (reverse proxy/TLS), Bean Validation

---

## RESUME BULLETS (pick 4–6, tune to the role)

- Built a **geospatial venue-discovery API** using PostgreSQL + PostGIS,
  writing native `ST_DWithin`/`ST_Distance` queries with GIST indexing to
  return nearby courts ranked by distance in <10 ms on 100k+ seeded rows.
- Designed a **concurrency-safe court-booking system** using time-range
  overlap detection guarded by **JPA pessimistic row locking**
  (`SELECT … FOR UPDATE`) so concurrent booking requests for the same court
  serialize instead of double-booking; covered by integration tests.
- Implemented **JWT authentication with rotating refresh tokens** (SHA-256
  hashed at rest, single-use, automatic theft detection), timing-safe login,
  Google OAuth2 sign-in, and HaveIBeenPwned breach-password screening per NIST
  SP 800-63B.
- Modeled a **normalized relational schema** (15+ entities) including
  composite-key join tables (`@EmbeddedId`/`@MapsId`) for user-sport
  preferences and match participants, and a one-to-one booking↔match link.
- Engineered a **match-history + Elo rating engine**: per-sport ratings,
  two-party result verification, dispute handling, and idempotent rating
  application guarded against double-processing.
- Built a **venue-owner analytics dashboard**: revenue rollups, an hour-of-week
  occupancy heatmap, a "revenue-left-on-the-table" report, and **"Smart Fill"**
  — a geospatial push system that notifies the nearest matchmaking-eligible
  players when a slot opens, with per-court rate-limiting to prevent spam.
- Hardened the API to **OWASP ASVS** controls: centralized exception handling,
  CORS allow-listing, security headers (HSTS/CSP), IDOR-proof authorization,
  mass-assignment defense, and request-size/pagination caps.
- Achieved **~90 automated tests** (unit + Testcontainers integration) gated by
  **GitHub Actions CI** running the full suite against a real PostGIS container
  on every push.

---

## SYSTEM OVERVIEW (for "walk me through your project")

**Architecture:** Layered Spring Boot monolith — Controller → Service →
Repository — with DTOs at every boundary (entities never serialized directly).
Stateless JWT auth so the app scales horizontally; Redis/Redisson coordinates
state that must be consistent across instances (booking locks, refresh-token
rotation).

**Two user modes, one codebase:** *Players* discover/book courts, log matches,
build Elo + reliability; *Venue owners* ("host mode") run an operations
dashboard. Role-based access via Spring Security `@PreAuthorize` plus
per-resource ownership checks in the service layer.

---

## STANDOUT FEATURES TO TALK ABOUT IN INTERVIEWS

### 1. Geospatial search (PostGIS)
Venue locations stored as `geometry(Point, 4326)`. Native queries use
`ST_DWithin` (index-friendly radius filter) + `ST_Distance` (ordering), backed
by a **GIST index**. Talking points: why a B-tree can't index 2D points, why
`ST_DWithin` uses the index but `ST_Distance` in a WHERE clause wouldn't, the
lng/lat ordering trap, and SRID 4326.

### 2. Concurrency-safe booking
The classic "two users book the same slot at once" race. Solution: overlap
detection (`start < otherEnd AND end > otherStart`) executed **after taking a
JPA pessimistic write lock** (`SELECT … FOR UPDATE`) on the court row, so
concurrent `create()` calls serialize at the database. Talking points: why a
DB unique constraint alone is insufficient for multi-row time ranges,
pessimistic vs optimistic locking trade-offs, and how the lock scope is kept
to the court row to minimize contention. (Redisson is a project dependency for
future cross-instance coordination but the current design relies on the DB
lock, which is sufficient for a single Postgres primary.)

### 3. Refresh-token rotation + theft detection
Short-lived access tokens (15 min) + long-lived refresh tokens. Refresh tokens
are **hashed (SHA-256) at rest** and **single-use**: each refresh rotates the
token. Re-use of an already-rotated token signals theft → all sessions
revoked. Password change → revoke everything.

### 4. Smart Fill (the product differentiator)
When a court goes empty, one tap notifies the *nearest* players who play that
sport, are open to matchmaking, are available that day type, and whose
last-known location is within their own notification radius — all in one
PostGIS query ordered by distance. Rate-limited per court per day; every offer
recorded for dedup + future conversion analytics.

### 5. Composite-key domain modeling
`UserPreference` (PK = user_id + sport_id) and `Participants` (PK = match_id +
user_id) use `@EmbeddedId` + `@MapsId`. Talking point: why these are
many-to-many *with attributes*, and how `@MapsId` reuses the FK column as part
of the PK instead of creating a duplicate.

---

## DATABASE DESIGN HIGHLIGHTS

- 15+ entities; UUID primary keys throughout.
- Composite-key join tables for user-sport prefs and match participants.
- One-to-one Booking↔Match (a booking can produce a logged match).
- JSONB for flexible venue opening-hours; `text[]` element-collections for
  amenities.
- PostGIS `Point` geometry + GIST index for spatial queries.
- Enum-as-string columns with DB CHECK constraints (booking/payment/match
  statuses) for type safety.
- Idempotency + audit columns (`ratings_applied`, `no_show_marked_at`,
  `confirmed_at`, `cancelled_by`).

---

## ENGINEERING PRACTICES (signals seniority)

- **Test pyramid:** fast Mockito unit tests for logic + Testcontainers
  integration tests that spin up real PostGIS (no H2 fakery for spatial code).
- **CI/CD:** GitHub Actions runs `mvn verify` (unit + integration) on every
  push/PR; weekly OWASP dependency-check for CVEs.
- **Security-first:** centralized `@RestControllerAdvice`, typed exception
  hierarchy mapped to correct HTTP codes, no stack-trace leakage, ASVS-aligned
  headers/CORS/validation.
- **Seed tooling:** a profile-gated data seeder generates 100k+ realistic rows
  for performance testing (running `EXPLAIN ANALYZE` to verify index usage).
- **Collaboration:** Git fork/PR workflow with a teammate; resolved multi-file
  merge conflicts integrating two parallel feature tracks.

---

## METRICS TO QUOTE

- ~90 automated tests, full suite green in CI (~3 min incl. container startup).
- 15+ JPA entities, 11 repositories.
- 8 vendor-dashboard endpoints + full player API surface.
- Sub-10 ms nearby-venue queries on 100k-booking seeded dataset.

---

## HONEST FRAMING NOTE

This was a **two-person project**. On a resume, scope your bullets to what you
personally built (e.g. "Built the geospatial discovery, matchmaking, and
vendor-analytics services" rather than implying solo authorship of everything).
Interviewers respect clear ownership boundaries and will ask "what did *you*
build" — have a crisp answer.
