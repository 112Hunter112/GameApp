# Handoff — Findings & Recommendations (Match History + Feed)

**Branch:** `claude/test-cases-rebased-fork-tv9dil`
**Audience:** the team taking over match-history and feed work.
**Status:** findings below were traced to source and, where noted, verified
against a real Postgres + Redis. Nothing in the match/feed *business logic*
was changed while producing this document — these are open items for you to
action. (What *was* changed this week: added tests, the social-feed feature,
DB indexes, the CI/CD pipeline, and the perf harness — see the git log.)

How to read severity:
- **P0 — launch blocker.** Breaks a core flow on real data. Fix before any public use.
- **P1 — high.** Integrity/security/data-corruption; fix early.
- **P2 — medium/low.** Correctness, performance, or UX gaps; schedule.
- **Product decision.** Needs a human call, not just a code fix.

A recurring root cause worth internalizing: **the existing tests mock the
repositories**, so lazy-loading and transaction-boundary bugs are invisible to
CI. Several P0/P1 items below are green in the current suite and still 500 in
production. The single most valuable process fix is a **DB-backed integration
test** (Testcontainers, like `BookingConflictIT`/`FeedShareCacheIT`) for the
match endpoints.

---

## P0 — Launch blockers

### P0-1. Match verification and four dashboard reads 500 on real data
`spring.jpa.open-in-view: false` (`application.yml:32`) + `Match.participants`
is LAZY (`Match.java:69`). Any service method that maps a match through
`MatchMapper.toDto` (which reads `getParticipants()`, `MatchMapper.java:47`)
**without `@Transactional`** throws `LazyInitializationException` → HTTP 500.

Missing the annotation:
- `verifyMatch` (`MatchService.java:375`) — **match verification is entirely dead;
  no manual match can leave PENDING.**
- `getUpcomingMatches` (`:277`), `getMatchesToday` (`:283`),
  `getPendingVerifications` (`:296`), `getMatchesWithoutResults` (`:302`).

Their siblings (`allMatches:57`, `getMonthlyStats:229`, `getCurrentStreak:251`,
`getHeadToHead:311`, `cancelMatch:353`, `updateMatchScore:429`) *are* annotated
and work — the omission is accidental. **Fix:** add `@Transactional` (`readOnly=true`
for the reads). Then add a Testcontainers IT that calls these through the real
mapper so the regression can't reappear. Passes CI today because tests inject
`participants` as plain `List.of(...)`, which never lazy-loads.

### P0-2. Competitive stats are forgeable; verification protects nothing
The stats/streak/head-to-head queries filter only on participant id — **no
`verificationStatus` filter**: `countTotalMatches` (`MatchRepository.java:87`),
`countTotalWins` (`:198`), `findCompletedMatchesByParticipant`/streak (`:208`),
`findHeadToHead` (`:81`), `findMatchesThisMonth` (`:99`), `findHistory` (`:72`).

A brand-new match counts the instant it is created — and the creator fully
controls `winningTeam` at creation (`MatchService.java:99`) with no opponent
consent and no rate limit. So: fabricate N matches with `winningTeam=TEAM_A`
against throwaway emails → wins, win-rate, and streak inflate immediately; a
match an opponent later **REJECTED still counts as a win**. **Fix:** add
`AND m.verificationStatus = 'CONFIRMED'` to every aggregation query; only
confirmed results should count anywhere.

---

## P1 — High (security & integrity)

### P1-1. Any participant can rewrite score AND winner, even after CONFIRMED
`updateMatchScore` (`MatchService.java:429`) checks only "is a participant,"
then sets `score`/`winningTeam` verbatim — no state guard, no ownership beyond
participation, `MatchValidator` never invoked. The loser can flip a confirmed
loss to a win (it resets to PENDING but the attacker-chosen values persist);
`winningTeam="TEAM_C"` or garbage scores are accepted and silently poison the
string-equality stats. **Fix:** restrict to creator (or a real dispute flow),
block edits once `verificationStatus != PENDING`, and run it through
`MatchValidator`.

### P1-2. Self-verification bypass via attacker-controlled invite email
Inviting an email creates a real, resettable "shadow user"
(`MatchService.java:141-153`; password stored as a **raw UUID, not bcrypt**,
`:148`), and `PasswordResetService.resetPassword` has **no `isVerified` gate**.
Attacker invites an address they own (e.g. Gmail plus-alias) as the opponent,
resets that shadow account's password, logs in, and verifies their own
fabricated win (`verifyMatch` only blocks the creator and same-team, not an
opponent-seat the attacker controls). **Fix:** don't let invite-created accounts
verify the match that created them; gate reset/login on `isVerified`; prefer a
signed `externalVerificationToken` (already a column on `Match`, currently
unused) over a full account.

### P1-3. Email squatting / registration lock-out
Inviting `victim@x.com` inserts a permanent `users` row with that email
(`MatchService.java:153`); registration later rejects it via `existsByEmail`
→ the real owner can never sign up. **Fix:** keep invited/external opponents out
of `users` (dedicated `pending_invite` table, or the unused
`external_opponent_*` columns), reconcile at real signup.

### P1-4. Invite path enables email/notification spam (no rate limit)
`POST /api/matches/manual` emails any attacker-supplied address an app-branded
invite (`EmailService.sendInvite`, called `MatchService.java:156`) and fans
`sendMatchInvite` to any list of real UUIDs — no throttle anywhere in the
codebase. **Fix:** rate-limit match creation and invites per user/IP; cap
invited emails per request/day; require a relationship before inviting real
users.

### P1-5. Participant emails (and bios) leak in every match response
`MatchMapper.toUserSummary` sets `email`, `bio`, `profilePictureUrl`
(`MatchMapper.java:88`), serialized in every `MatchResponse`. A UUID→email
harvester for anyone you've shared one match with. **Fix:** expose id + display
name + avatar only, matching the deliberately-minimal `MatchImageResponse`.

### P1-6. Feed sharing ignores privacy, verification, and consent
`FeedShareService.share` (`:102`) checks only participation. It does **not**
check `match.isPrivate()` (`Match.java:49`), `verificationStatus`, or the
sharer's own `ParticipationStatus`. So a **private** match, a **PENDING/REJECTED**
result, or a match you were merely *invited* to can be broadcast to your
friends — exposing every participant's name and the score
(`FeedShareService.java:235`) to people who may not know them. **Fix:** require
`isPrivate == false`, `verificationStatus == CONFIRMED`, and an ACCEPTED
participation before allowing a share.

### P1-7. `cancelMatch` FK violation when the match has a photo
`cancelMatch` deletes participants, feed shares, then the match, but never the
`match_images` (`MatchImage.match` is `nullable=false`, no cascade,
`MatchImage.java:24`). Attach an image then cancel → `DataIntegrityViolationException`
→ 409, match permanently un-cancellable. **Fix:** add `deleteByMatch_Id` to
`MatchImageRepository` and call it in `cancelMatch` (or cascade).

---

## P1 — High (concurrency), introduced with the feed feature this week

### P1-8. Feed cache evicted *before* commit (own-screen guarantee is broken)
`share`/`unshare`/`onTargetDeleted` call `evictFeedPages` **inside** the
`@Transactional` method (`FeedShareService.java:131,147,158`), i.e. before the
commit. Under READ COMMITTED a concurrent feed read between evict and commit
re-caches the pre-commit state, so the sharer can see stale content for the
full TTL — contradicting the class comment's promise. The single-threaded
`FeedShareCacheIT` doesn't catch it. **Fix:** evict in an after-commit
synchronization (`TransactionSynchronizationManager.registerSynchronization`
with `afterCommit`, or a `@TransactionalEventListener(phase = AFTER_COMMIT)`).

### P1-9. Feed hydration N+1 on participant names
`toItem` reads `match.getParticipants()` per match (`FeedShareService.java:236`);
matches are batch-loaded but their participant collections are lazy → ~20 extra
queries per uncached page (the "no N+1" comment is wrong for this path).
**Fix:** batch-load participant display names for all page matches in one query
(projection), or denormalize a short participant summary onto the share.

---

## P2 — Medium / Low

- **`@Async` is inert.** No `@EnableAsync` anywhere, but `EmailService.sendInvite`
  is `@Async` (`EmailService.java:84`) → it runs **synchronously inside the
  match-creation transaction**. Slow/failed SMTP rolls back match creation, or
  an email goes out for a match that then rolls back. See the async note below.
- **`opponentIds` + `opponentEmails` not cross-checked** → pass a UUID and that
  user's email in one request → duplicate `(match_id, user_id)` PK → 500
  (`MatchService.java:135`). Also a check-then-act race on shadow-user creation.
- **`getCurrentStreak` paginates in memory** — `JOIN FETCH` + `Pageable`
  (`MatchRepository.java:208`) triggers Hibernate HHH90003004: loads *all* the
  user's completed matches then trims to 100; long streaks silently cap.
- **Create-match returns `participants: []`** — persisted via `entityManager.persist`
  but never attached to the returned entity (`MatchService.java:187`).
- **`updateMatchScore` doesn't reopen a REJECTED match** — only CONFIRMED→PENDING
  is handled (`:442`); a corrected REJECTED score stays REJECTED, no re-verify
  notification.
- **`verifyMatch` is non-transactional and has no state guard** — flip-flop
  CONFIRMED↔REJECTED, non-atomic multi-save (`MatchService.java:375-427`). Add
  `@Transactional`, require PENDING, make transitions one-way.
- **`ratingsApplied` never set/checked** (`MatchService.java:410` TODO) — when
  Elo lands, re-confirmation double-applies. Add `@Version` to `Match` first.
- **`deleteImage` ignores `{matchId}`** (`MatchImageController.java:55`) — can
  delete an image of a different match (uploader check still holds).
- **`getPendingVerifications` shows teammates unactionable cards** (verify 403s
  teammates, `MatchService.java:398`).
- **Dangling `MATCH_INVITE` notifications after cancel** — `referenceId` has no
  FK; tapping a stale invite → 404.
- **Head-to-head 500 for unknown opponent** — bare `orElseThrow()`
  (`MatchService.java:319`) → `NoSuchElementException` → 500 instead of 404; also
  leaks any user's name for an arbitrary UUID.
- **`getMostPlayedOpponents` unbounded `limit`** and **match-history unbounded
  `size`** — both build `PageRequest.of(...)` by hand from raw params
  (`MatchController.java:58,147`), bypassing `spring.data.web.pageable.max-page-size`.
  `?size=100000000` → load everything.
- **VENUE_OWNER cannot log manual matches** — `@PreAuthorize("hasAnyRole('USER','ADMIN')")`
  excludes `ROLE_VENUE_OWNER` (`MatchController.java:45`); likely unintended.
- **Feed pagination has no tiebreaker** — orders by `created_at` only
  (`FeedShareRepository.java:16`); same-tick shares skip/duplicate across offset
  pages. Prefer keyset pagination `(created_at, id)`.
- **Unshare not idempotent** — deleting an already-gone share → 404
  (`FeedShareService.java:138`); confusing after the underlying match was cancelled.
- **Dead repo method** `findByParticipants_Id_UserIdAndParticipants_Status(UUID, String)`
  takes `String` for an enum property (`MatchRepository.java:51`); unused, will
  fail if wired up.

---

## Missing pieces — product decisions (not just code)

- **No dispute *reason* and no resolution flow.** `DISPUTED` status exists but is
  unreachable (verify only sets CONFIRMED/REJECTED); there's no reason field and
  no `GET /api/matches/{id}`. The loser of a wrong score has no structured recourse.
- **REJECTED is a dead end** — can't be deleted (cancel is PENDING-only) or reopened.
- **No block feature** — `FriendshipStatus.BLOCKED` is defined but never assigned;
  "they blocked you" is unrepresentable, and feed/visibility can't honor it.
- **No account deletion / GDPR export / bulk-history delete** — you store DOB,
  gender, photos, emails. Required before public launch in most jurisdictions.
- **Feed has no reactions, comments, or share-notifications**; sharing anything
  other than a MATCH is unsupported (`FeedTargetType` has one value).
- **External-opponent email verification is unbuilt** — matches whose only
  opponent is an off-app email can never be verified (independent of P0-1).

---

## Performance findings (from the stress harness in `perf/`)

Validated at scale (1M bookings, 2M notifications, then 4M matches / 8M
participations with a hot user carrying ~20k matches). Full numbers in
`perf/RESULTS.md`.

**Indexes applied this week (entity annotations = prod schema under
`ddl-auto: update`; all pinned by `SchemaHotPathGuardTest`):**
- `notifications (recipient_id, is_read)` and `(recipient_id, created_at)` —
  the polled bell badge was a 124ms full scan, now 1.3ms.
- `bookings (user_id, start_time)` — player views.
- `court_blocks (court_id, start_time, end_time)` — overlap check under the lock.
- `password_reset_codes (user_id, created_at)`.
- `participants (user_id, match_id)` — **essential**: match history was 376ms →
  120ms for a 20k-match user, 3.5ms for a typical 173-match user.
- `matches (match_date)` — the date-ordered family; 20k-match tail 120ms → 38ms.
- `feed_shares (user_id, created_at)` + `(target_type, target_id)` + unique
  `(user_id, target_type, target_id)`.

**Still open (need code, not just an index):**
- **`findFriendsRecentActivity` ≈ 1s** and un-indexable — it's superseded by the
  new `feed_shares` table. Retire it or hard-bound it to a recent window + small limit.
- **Feed hot-sharer page = 251ms uncached** (vs 1.3ms typical) — the Redis cache
  is load-bearing; at much larger scale move to fan-out-on-write.
- **Deep-offset pagination** (feed and notifications) — switch to keyset.
- **`CourtBlockService` race** was fixed (now takes the court lock); booking
  double-book protection is solid (316 tps serialized on one hot court, zero
  double-books under a 6-thread race).

---

## Note: async / concurrency model for Java (asked: "async & promises like TypeScript?")

Short answer: **yes, selectively — but Java's model is different from JS, and
this codebase should use a small, targeted amount, not a rewrite.**

Java equivalents of JS `async`/`await`/`Promise`:
- **`CompletableFuture<T>`** — the direct `Promise` analog: `.thenApply`,
  `.thenCompose` (≈ `.then`), `.exceptionally` (≈ `.catch`), `allOf` (≈
  `Promise.all`). Use for composing a few independent I/O calls.
- **Spring `@Async`** — annotate a method, it runs on a thread pool and returns
  `void`/`CompletableFuture`. Best for **fire-and-forget side effects**.
- **Virtual threads (Project Loom, Java 21+)** — the biggest lever for an
  I/O-bound Spring MVC app: write ordinary blocking code, the runtime makes it
  cheap to have thousands of concurrent requests. **This project is on Java 17**,
  so it's a version-bump decision, not a code style.
- **Reactive (WebFlux/Reactor `Mono`/`Flux`)** — full non-blocking stack. Powerful
  but invasive and hard to debug; **not recommended here** — it would fight the
  JPA/blocking-JDBC design you already have.

**Concrete recommendations for this codebase, in order:**
1. **Turn on the async you already have.** Add `@EnableAsync` (a
   `@Configuration` with a bounded `ThreadPoolTaskExecutor`) so the existing
   `@Async` on `EmailService` actually moves SMTP off the request thread. This
   alone fixes the P2 "email blocks match creation" item. **Do this first.**
2. **Only make truly fire-and-forget work async: emails, push notifications,
   Smart-Fill pings.** Never make a method async if the caller needs its result
   to build the HTTP response.
3. **Async does NOT mix with `@Transactional` naively.** An `@Async` method runs
   on a different thread with **no transaction and no Hibernate session** — the
   exact lazy-loading trap that causes P0-1. Rule: pass fully-loaded DTOs/plain
   values into async methods, never lazy JPA entities; do all DB work in the
   caller's transaction, then hand off.
4. **For read endpoints that fan out to several independent queries** (e.g. a
   dashboard aggregating stats + upcoming + streak), `CompletableFuture` with a
   bounded executor can cut latency — but measure first; most of these are already
   sub-millisecond with the new indexes.
5. **When you move to Java 21**, prefer **virtual threads** over reactive for
   scaling concurrent requests — same simple blocking code, far more throughput,
   none of the reactive debugging tax.

Anti-pattern to avoid: sprinkling `@Async`/`CompletableFuture` widely "to be
fast." It multiplies the transaction/lazy-loading bugs this report is already
full of. Keep the request path synchronous and transactional; push only
side effects off-thread.

---

## Observability & error logging (do before public launch)

Today there is **no request correlation** anywhere (no MDC/trace id), and no
structured logging. The `GlobalExceptionHandler` is a good foundation — it
already funnels everything to one place and logs unexpected 5xx with a stack
trace (`GlobalExceptionHandler.java:120`) and DB conflicts as WARN — but when a
user reports "I got an error," you currently cannot map that report to their
request's logs. Do **not** fix this by logging at every step (noise, cost, and
a PII-leak risk given P1-5); add these instead:

1. **Correlation id (highest value).** A `OncePerRequestFilter` that
   reads/generates `X-Request-Id`, puts it in the SLF4J **MDC** (so every log
   line for the request carries it automatically), returns it as a response
   header, and includes it in the error JSON body (extend `baseBody`). Then a
   user's "error `a1b2c3`" maps to their whole request in one CloudWatch query.
2. **Context on the 5xx log line.** Add path, method, and authenticated user id
   to the catch-all `log.error` so one line is self-sufficient.
3. **Structured (JSON) logging under a `prod` Spring profile** via a
   `logback-spring.xml` (JSON in prod, plain console in dev) — needs the
   `logstash-logback-encoder` dependency — so CloudWatch Logs Insights can query
   by field (`status`, `requestId`, `path`, `userId`) instead of scraping text.
4. **Level discipline:** unexpected 5xx = ERROR with stack + context; handled 4xx
   domain exceptions = do not log; DB conflicts = WARN (already the case).
5. **Never log** request bodies, emails, tokens, or passwords (see P1-5).

## Operational launch-readiness (infra/process, not app code)

Cross-cutting items required before public use, independent of the match/feed bugs:

- **Rotate the credentials in `TEST_CREDENTIALS.md` and remove the file.** It sits
  in a public repo with real-looking account passwords, and it is in git history —
  rotation matters more than deletion. (**P0-severity for a public repo.**)
- **Database backups.** One droplet + one Postgres volume + no dumps = one disk
  failure from total data loss. Add a nightly `pg_dump` to off-box storage (S3/B2)
  and practice one restore.
- **Schema migrations.** `ddl-auto: update` (`application.yml:24`) mutates prod
  schema on boot with no review — dangerous once multiple people (and the planned
  KeshavAditya fork merge) change entities. Adopt **Flyway** before that merge.
- **Auth rate limiting.** No brute-force protection on `login`/`register`/
  forgot-password, nor on match creation/invites (see P1-4). Add Caddy
  `rate_limit` or bucket4j.
- **Monitoring.** No uptime check, error alerting, or log rotation today. Add an
  uptime pinger + Sentry (or equivalent) + logrotate so a 3am crash isn't silent.
- **Email deliverability.** SMTP via a duckdns host will land invites/reset codes
  in spam. Move to SES/Resend on a real domain with SPF/DKIM/DMARC.
- **Legal / privacy.** You store DOB, gender, photos, and emails — a privacy
  policy, ToS, and an **account-deletion + data-export** path are legally required
  in most jurisdictions (ties to the GDPR item under product decisions).
- **Beta smoke test in the pipeline.** Add an automated end-to-end smoke run
  (register → login → search → book → cancel) against the beta environment in
  `deploy.yml`, gating the production approval — the one piece the staged pipeline
  is missing.

## Suggested fix order

1. `@Transactional` on the five match methods + a **DB-backed IT** (P0-1).
2. `verificationStatus = CONFIRMED` filter on stats; lock down `updateMatchScore` (P0-2, P1-1).
3. Drop `email` from the match mapper (P1-5).
4. Shadow-user redesign — closes self-verify + squatting + spam together (P1-2,3,4).
5. `@EnableAsync` + move email off the request thread (P2 async item).
6. Feed: after-commit eviction + batch participant names + privacy/status checks on share (P1-6,8,9).
7. `cancelMatch` image cleanup; page-size caps; then the P2 list.
8. Product calls: dispute flow, block feature, account deletion.
