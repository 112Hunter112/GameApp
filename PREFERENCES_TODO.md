# User Preferences — TODO

A checklist for everything left to build around the `UserPreference` feature.
Ordered roughly by dependency — earlier sections unblock later ones.

---

## What's already done ✅

- [x] `UserPreference` entity with full field set (Elo, matches, opponent filters,
      travel distance, primary-sport flag, timestamps)
- [x] `UserPreferenceId` composite key (`@Embeddable` with `userId` + `sportId`)
- [x] `UserPreferenceRepository extends JpaRepository<UserPreference, UserPreferenceId>`
- [x] One method: `findByUserId(UUID userId)`

---

## 1. Expand `UserPreferenceRepository`

The current repo only has one method. Add the ones that the service layer
will actually need:

- [ ] `Optional<UserPreference> findByUser_IdAndSports_Id(UUID userId, UUID sportId)`
      — used for "do I already have a preference for this sport?" and for updates.
- [ ] `boolean existsByUser_IdAndSports_Id(UUID userId, UUID sportId)`
      — quick existence check before insert.
- [ ] `Optional<UserPreference> findByUser_IdAndIsPrimarySportTrue(UUID userId)`
      — used to find current primary so it can be unset when changing primaries.
- [ ] `long countByUser_Id(UUID userId)`
      — for "you have N preferences set up" UI.
- [ ] `void deleteByUser_IdAndSports_Id(UUID userId, UUID sportId)`
      — for the DELETE endpoint.
- [ ] **Matchmaking candidate query** (the big one):
      ```java
      @Query("""
          SELECT p FROM UserPreference p
          WHERE p.sports.id   = :sportId
            AND p.user.id     <> :requesterId
            AND p.openToMatchmaking = true
            AND (:minElo IS NULL OR p.eloRating >= :minElo)
            AND (:maxElo IS NULL OR p.eloRating <= :maxElo)
          """)
      Page<UserPreference> findMatchCandidates(
          @Param("requesterId") UUID requesterId,
          @Param("sportId")     UUID sportId,
          @Param("minElo")      Integer minElo,
          @Param("maxElo")      Integer maxElo,
          Pageable pageable);
      ```

---

## 2. Build the DTO layer

The frontend should never see the raw entity. Create:

- [ ] **`dto/UserPreferenceRequest.java`** — what the client sends on create/update.
      Required: `sportId`. Optional everything else with sensible defaults server-side.
      Validate `@NotNull` on `sportId`, `@Min(0)` on Elo bounds, etc.
- [ ] **`dto/UserPreferenceResponse.java`** — what the API returns.
      Flatten the sport: instead of returning the nested `Sports` object, include
      `sportId` (UUID) and `sportName` (String) directly so the frontend doesn't
      have to fetch sports separately.
- [ ] **`dto/PlayerCandidateResponse.java`** — for matchmaking results.
      Fields: user id, first/last name, username, profile picture URL, age (computed
      from `dateOfBirth`), Elo for the matched sport, optional `distanceMeters` if
      a location was provided.

---

## 3. Build `UserPreferenceService`

New file: `service/UserPreferenceService.java`. Methods:

- [ ] `List<UserPreferenceResponse> listMine(UUID userId)`
      — calls `findByUserId`, maps to DTO.
- [ ] `UserPreferenceResponse upsert(UUID userId, UserPreferenceRequest req)`
      — single endpoint that creates or updates depending on existence.
      Inside:
      - validate sport exists via `SportsRepository.findById`
      - build the `UserPreferenceId` from userId + sportId
      - if exists → update fields. If not → create new with Elo defaulting to 1200.
      - save, return mapped DTO.
- [ ] `void setPrimary(UUID userId, UUID sportId)`
      **Must be `@Transactional`** because it's two writes:
      1. Unset whatever was previously primary for this user.
      2. Set the new one as primary.
      If the requested preference doesn't exist, throw a typed exception.
- [ ] `void delete(UUID userId, UUID sportId)`
      — straight delete.
- [ ] Internal helper: a mapper method `toResponse(UserPreference)` that flattens
      the sport.

**Concept to make sure you understand here:** `@Transactional` self-invocation —
if `setPrimary` calls another `@Transactional` method on `this`, the second
transaction does **not** start because the call bypasses the Spring proxy.
Keep both updates inside the same method body or extract to a different bean.

---

## 4. Build `UserPreferenceController`

New file: `controller/UserPreferenceController.java`, base path `/api/me/preferences`:

- [ ] `GET  /api/me/preferences` → list current user's preferences.
- [ ] `PUT  /api/me/preferences` → upsert. Body: `UserPreferenceRequest`.
- [ ] `POST /api/me/preferences/{sportId}/primary` → set as primary.
- [ ] `DELETE /api/me/preferences/{sportId}` → delete one.

**Security notes:**
- Class-level `@PreAuthorize("isAuthenticated()")` so anonymous can't hit it.
- Pull the user id from the auth principal (`@AuthenticationPrincipal`) — never
  trust a userId in the path or body. The user can only manage their own
  preferences.
- Add `@Validated` so request body validation actually fires.

**Don't add `/api/users/{userId}/preferences` paths** — they would let one user
read or modify another user's settings unless every method checks ownership.
Keep it strictly `me`.

---

## 5. Personalized nearby venues (uses preferences)

In `VenueRepository`:

- [ ] `findNearbyForUserSports(lat, lng, radius, sportIds, pageable)`
      — same as `findNearbyWithDistance` plus
      `JOIN courts c ON c.venue_id = v.id JOIN sports s ON s.id = c.sports_id`
      and `WHERE s.id IN :sportIds`.

In `VenueService`:

- [ ] `findNearbyForMe(userId, lat, lng, radius, pageable)`
      — load user's preferences, extract sport IDs where
      `openToMatchmaking = true`, delegate to the new repo method.
      Decide behavior when the user has zero preferences: return all nearby
      venues, or empty? Document the choice in the service Javadoc.

In `VenueController`:

- [ ] `GET /api/venues/nearby/me?lat=...&lng=...&radiusMeters=...`

---

## 6. Matchmaking (the home-screen "people to play with")

New files:

- [ ] `service/MatchmakingService.java`
      - `findCandidates(UUID userId, UUID sportId, Pageable pageable)`
      - load the requester's preference for the sport (so we know their
        `min_opponent_elo` / `max_opponent_elo`)
      - call `userPreferenceRepository.findMatchCandidates(...)`
      - map each candidate to `PlayerCandidateResponse`
      - **age filtering**: candidates with `dateOfBirth` outside the requester's
        `[min_opponent_age, max_opponent_age]` should be filtered out. Do this in
        the service for now (cleaner than dragging birth date into the SQL).
- [ ] `controller/MatchmakingController.java`
      - `GET /api/matchmaking/candidates?sportId=...` → page of candidates.

---

## 7. Elo updates after match completion

This bridges two systems — wires `MatchService` to write into `UserPreference`.

- [ ] Add a method on `UserPreferenceService`:
      `void recordMatchResult(UUID userId, UUID sportId, int opponentElo, double score)`
      where `score = 1.0` (win), `0.5` (draw), `0.0` (loss).
      Inside:
      - load preference; if missing, auto-create with defaults so first-time players
        still get a rating.
      - compute new Elo: `newElo = oldElo + K * (score - expected)`
        where `expected = 1 / (1 + 10^((opponentElo - oldElo) / 400))` and `K = 32`.
      - increment `matchesPlayed`; conditionally `matchesWon`.
      - save.
- [ ] In `MatchService`, hook into the moment a match's
      `verification_status` transitions to `CONFIRMED`.
      For each participant, look up their opponent's Elo and call the service
      method above.
- [ ] **Idempotency:** add a boolean `ratings_applied` column on `matches`
      so that re-confirming a match doesn't double-apply Elo.
      Wrap the whole thing in `@Transactional` so partial updates can't leave
      one player rated and the other not.

---

## 8. Database safeguards

Optional but cheap:

- [ ] **Partial unique index** on primary sport:
      ```sql
      CREATE UNIQUE INDEX one_primary_per_user
        ON user_preferences (user_id)
        WHERE is_primary_sport = true;
      ```
      Prevents a bug in `setPrimary` from ever creating two primaries.
- [ ] **Check constraints** for sanity:
      ```sql
      ALTER TABLE user_preferences
        ADD CONSTRAINT elo_in_range CHECK (elo_rating BETWEEN 0 AND 4000),
        ADD CONSTRAINT age_range_valid CHECK
            (min_opponent_age IS NULL OR max_opponent_age IS NULL
             OR min_opponent_age <= max_opponent_age),
        ADD CONSTRAINT elo_range_valid CHECK
            (min_opponent_elo IS NULL OR max_opponent_elo IS NULL
             OR min_opponent_elo <= max_opponent_elo);
      ```

---

## 9. Tests (do these before the codebase grows)

- [ ] Unit: Elo math (`recordMatchResult`) — test win, loss, draw, and the
      symmetry that swapping (winner, loser) negates the rating change.
- [ ] Unit: `setPrimary` correctly unsets previous primary.
- [ ] Unit: `upsert` defaults Elo to 1200 on first insert.
- [ ] Integration (Testcontainers + PostGIS image): `findMatchCandidates`
      returns exactly the rows that match all filters.
- [ ] Integration: full create → update → setPrimary → delete via MockMvc.

---

## 10. Frontend payload — design once, build fast

Before the controllers go live, decide:

- [ ] **Home screen DTO** = lean. Just `id`, `sportName`, `eloRating`,
      `matchesPlayed`. Don't dump the full preference object on the home page.
- [ ] **Settings screen DTO** = fat. Everything the user can edit.
- [ ] **Matchmaking DTO** = candidate info only. Never expose the other user's
      private preferences (their `min_opponent_elo` etc. should not leak).

This is a UI decision but it changes which fields each endpoint returns.
Decide before writing the mapper.

---

## Suggested build order (one focused session each)

1. **Section 1** — repo methods (15 min)
2. **Section 2** — DTOs (30 min)
3. **Section 3** — service (60 min)
4. **Section 4** — controller (45 min)
5. ✋ **Commit. Smoke test with curl.**
6. **Section 5** — personalized nearby (60 min)
7. **Section 6** — matchmaking (90 min)
8. **Section 7** — Elo on match completion (60–90 min)
9. **Section 9** — tests (90 min)
10. **Section 8** — DB safeguards (10 min, run once in psql)

Skip Section 8 until everything else works — it's hardening, not features.

---

## Concepts to refresh before coding each section

| Section | Concept worth re-reading |
|---|---|
| 1 | Spring Data method-name parsing rules — especially what `_` does for nested properties (`User_Id`). |
| 2 | Bean Validation: `@NotNull`, `@Min`, `@Valid` on request bodies. |
| 3 | `@Transactional` propagation + the self-invocation gotcha. |
| 4 | `@AuthenticationPrincipal` to pull the current user from the JWT filter. |
| 5 | Joining across multiple entities in JPQL vs. native SQL. |
| 6 | Interface projections OR DTO constructor expressions in JPQL. |
| 7 | Elo formula — read one explainer; it's 4 lines of math. |
| 9 | Testcontainers lifecycle (`@Testcontainers`, `@Container`, `@DynamicPropertySource`). |
