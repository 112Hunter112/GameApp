package com.parth.sportsapp.sportsbackend.config.devseed;

import com.parth.sportsapp.sportsbackend.model.*;
import com.parth.sportsapp.sportsbackend.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Performance-testing seed runner. ONLY active when the {@code seed} Spring
 * profile is enabled — invisible during normal {@code mvn test}, {@code mvn
 * verify}, or {@code docker compose up} runs.
 *
 * <h3>How to invoke</h3>
 * <pre>
 *   # local Maven run
 *   ./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
 *
 *   # already-built jar
 *   java -jar app.jar --spring.profiles.active=seed
 *
 *   # docker compose (set env on the app service)
 *   environment:
 *     - SPRING_PROFILES_ACTIVE=seed
 *     - APP_SEED_SIZE=MEDIUM
 * </pre>
 *
 * <h3>Sizes</h3>
 * <ul>
 *   <li><b>SMALL</b>  — 100 users, 50 venues, 500 bookings. Fast dev cycle.</li>
 *   <li><b>MEDIUM</b> — 1 000 users, 300 venues, 10 000 bookings. Realistic.</li>
 *   <li><b>LARGE</b>  — 5 000 users, 1 000 venues, 100 000 bookings. Stress test.</li>
 * </ul>
 *
 * <h3>Idempotency</h3>
 * The seeder skips itself if it detects existing seed-tagged users
 * ({@code email LIKE 'seed-%@perf.test'}). To wipe and re-seed:
 * {@code docker compose down -v && docker compose up}.
 *
 * <h3>How to measure</h3>
 * After seeding, point queries at the populated DB and inspect plans:
 * <pre>
 *   docker compose exec db psql -U postgres -d sportsapp -c "
 *     EXPLAIN ANALYZE
 *     SELECT 1 FROM bookings
 *     WHERE court_id = '...' AND start_time &lt; '2026-06-05 11:00';
 *   "
 * </pre>
 * If you see {@code Seq Scan on bookings} on a large table, you're missing
 * an index. {@code Index Scan using idx_xxx} means it's using one.
 */
@Component
@Profile("seed")
@Order(200) // run after SportsSeeder
public class DataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

  /** Metro centers used to spread venues across realistic geographies. */
  private static final double[][] METRO_CENTERS = {
      {41.8781, -87.6298},   // Chicago
      {40.7128, -74.0060},   // NYC
      {34.0522, -118.2437},  // LA
      {37.7749, -122.4194},  // SF
      {51.5074, -0.1278},    // London
      {28.6139, 77.2090},    // Delhi
      {19.0760, 72.8777},    // Mumbai
      {-33.8688, 151.2093},  // Sydney
  };

  private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

  private final UserRepository userRepository;
  private final SportsRepository sportsRepository;
  private final VenueRepository venueRepository;
  private final CourtRepository courtRepository;
  private final UserPreferenceRepository userPreferenceRepository;
  private final FriendshipRepository friendshipRepository;
  private final PasswordEncoder passwordEncoder;

  @PersistenceContext
  private EntityManager em;

  private final Sizes sizes;

  // Deterministic randomness so reseeded data has the same shape every run.
  private final Random rnd = new Random(42);

  public DataSeeder(UserRepository userRepository,
                    SportsRepository sportsRepository,
                    VenueRepository venueRepository,
                    CourtRepository courtRepository,
                    UserPreferenceRepository userPreferenceRepository,
                    FriendshipRepository friendshipRepository,
                    PasswordEncoder passwordEncoder,
                    @Value("${app.seed.size:SMALL}") String sizeName) {
    this.userRepository = userRepository;
    this.sportsRepository = sportsRepository;
    this.venueRepository = venueRepository;
    this.courtRepository = courtRepository;
    this.userPreferenceRepository = userPreferenceRepository;
    this.friendshipRepository = friendshipRepository;
    this.passwordEncoder = passwordEncoder;
    this.sizes = Sizes.fromName(sizeName);
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    // Idempotency check.
    if (userRepository.findByEmail("seed-0001@perf.test").isPresent()) {
      log.info("DataSeeder: seed data already present, skipping. " +
          "To re-seed: docker compose down -v && docker compose up");
      return;
    }

    List<Sports> allSports = sportsRepository.findAll();
    if (allSports.isEmpty()) {
      log.warn("DataSeeder: no Sports rows — run with the default profile first " +
          "so SportsSeeder can populate sports.json. Aborting.");
      return;
    }

    log.info("DataSeeder starting: size={} ({} users, {} venues, ~{} bookings, ~{} matches)",
        sizes.name(), sizes.users, sizes.venues, sizes.bookings, sizes.matches);
    long t0 = System.currentTimeMillis();

    String sharedPasswordHash = passwordEncoder.encode("seed-password");

    List<User> users = seedUsers(sharedPasswordHash);
    log.info(" .. {} users", users.size());

    List<Venue> venues = seedVenues(users);
    log.info(" .. {} venues", venues.size());

    List<Courts> courts = seedCourts(venues, allSports);
    log.info(" .. {} courts", courts.size());

    int prefs = seedUserPreferences(users, allSports);
    log.info(" .. {} user preferences", prefs);

    int bookings = seedBookings(users, courts);
    log.info(" .. {} bookings", bookings);

    int matches = seedMatches(users, allSports);
    log.info(" .. {} matches", matches);

    int friends = seedFriendships(users);
    log.info(" .. {} friendships", friends);

    log.info("DataSeeder done in {}s", (System.currentTimeMillis() - t0) / 1000);
  }

  // --- Users -----------------------------------------------------------------

  private List<User> seedUsers(String passwordHash) {
    List<User> batch = new ArrayList<>(1000);
    List<User> all = new ArrayList<>(sizes.users);
    for (int i = 0; i < sizes.users; i++) {
      User u = new User();
      String suffix = String.format("%04d", i);
      u.setFirstName("Seed" + suffix);
      u.setLastName("User");
      u.setEmail("seed-" + suffix + "@perf.test");
      u.setUsername("seed_" + suffix);
      u.setPassword(passwordHash);
      u.setRole(UserRole.USER);
      u.setVerified(true);
      u.setJoiningDate(LocalDateTime.now().minusDays(rnd.nextInt(365)));
      u.setUpdatedAt(LocalDateTime.now());
      u.setEmailNotificationsEnabled(true);
      u.setPushNotificationsEnabled(true);
      batch.add(u);
      if (batch.size() >= 1000) {
        all.addAll(userRepository.saveAll(batch));
        batch.clear();
        em.flush();
        em.clear();
      }
    }
    if (!batch.isEmpty()) all.addAll(userRepository.saveAll(batch));
    return all;
  }

  // --- Venues + Courts -------------------------------------------------------

  private List<Venue> seedVenues(List<User> users) {
    List<Venue> batch = new ArrayList<>(500);
    List<Venue> all = new ArrayList<>(sizes.venues);
    for (int i = 0; i < sizes.venues; i++) {
      double[] metro = METRO_CENTERS[i % METRO_CENTERS.length];
      // jitter ~0.4 deg (~ 40 km) — gives realistic spread within a metro
      double lat = metro[0] + (rnd.nextDouble() - 0.5) * 0.4;
      double lng = metro[1] + (rnd.nextDouble() - 0.5) * 0.4;
      Point p = GF.createPoint(new Coordinate(lng, lat));
      p.setSRID(4326);

      Venue v = new Venue();
      v.setOwner(users.get(rnd.nextInt(users.size())));
      v.setName("Seed Venue " + String.format("%04d", i));
      v.setAddress(rnd.nextInt(9999) + " Seed St");
      v.setPhoneNumber("+15551112222");
      v.setLocation(p);
      v.setActive(true);
      v.setManaged(true);
      v.setSource(VenueSource.REGISTERED);
      batch.add(v);
      if (batch.size() >= 500) {
        all.addAll(venueRepository.saveAll(batch));
        batch.clear();
        em.flush();
        em.clear();
      }
    }
    if (!batch.isEmpty()) all.addAll(venueRepository.saveAll(batch));
    return all;
  }

  private List<Courts> seedCourts(List<Venue> venues, List<Sports> sports) {
    List<Courts> batch = new ArrayList<>(1000);
    List<Courts> all = new ArrayList<>(venues.size() * 3);
    for (Venue v : venues) {
      // 1-4 courts per venue, varying sports
      int n = 1 + rnd.nextInt(4);
      for (int j = 0; j < n; j++) {
        Courts c = new Courts();
        c.setVenue(v);
        c.setSports(sports.get(rnd.nextInt(sports.size())));
        c.setCourtNumber("Court " + (j + 1));
        c.setHourlyRate(BigDecimal.valueOf(10 + rnd.nextInt(40))); // $10-$50/h
        c.setIndoor(rnd.nextBoolean());
        c.setActive(true);
        batch.add(c);
        if (batch.size() >= 1000) {
          all.addAll(courtRepository.saveAll(batch));
          batch.clear();
          em.flush();
          em.clear();
        }
      }
    }
    if (!batch.isEmpty()) all.addAll(courtRepository.saveAll(batch));
    return all;
  }

  // --- UserPreference (one row per user-sport pair) --------------------------

  private int seedUserPreferences(List<User> users, List<Sports> sports) {
    int count = 0;
    List<UserPreference> batch = new ArrayList<>(1000);
    for (User u : users) {
      // each user plays 1-3 sports
      int n = 1 + rnd.nextInt(3);
      Set<UUID> picked = new HashSet<>();
      for (int j = 0; j < n; j++) {
        Sports s = sports.get(rnd.nextInt(sports.size()));
        if (!picked.add(s.getId())) continue;

        UserPreferenceId pk = new UserPreferenceId();
        pk.setUserId(u.getId());
        pk.setSportId(s.getId());

        UserPreference p = new UserPreference();
        p.setId(pk);
        p.setUser(u);
        p.setSports(s);
        p.setEloRating(900 + rnd.nextInt(1600));   // 900-2500
        p.setMatchesPlayed(rnd.nextInt(100));
        p.setMatchesWon(rnd.nextInt(p.getMatchesPlayed() + 1));
        p.setOpenToMatchmaking(true);
        p.setAvailableWeekdays(rnd.nextBoolean());
        p.setAvailableWeekends(true);
        p.setIsPrimarySport(j == 0); // first picked = primary
        batch.add(p);
        count++;
        if (batch.size() >= 1000) {
          userPreferenceRepository.saveAll(batch);
          batch.clear();
          em.flush();
          em.clear();
        }
      }
    }
    if (!batch.isEmpty()) userPreferenceRepository.saveAll(batch);
    return count;
  }

  // --- Bookings (via EntityManager — no BookingRepository yet) ---------------

  private int seedBookings(List<User> users, List<Courts> courts) {
    int count = 0;
    LocalDate today = LocalDate.now();

    for (int i = 0; i < sizes.bookings; i++) {
      Booking b = new Booking();
      b.setUser(users.get(rnd.nextInt(users.size())));
      b.setCourt(courts.get(rnd.nextInt(courts.size())));

      // Distribute: 70% past, 30% future. Range past-90 days .. future+30 days.
      int dayOffset = rnd.nextBoolean()
          ? -rnd.nextInt(90)
          : (rnd.nextDouble() < 0.7 ? -rnd.nextInt(90) : rnd.nextInt(30));
      int hour = 6 + rnd.nextInt(15);         // 06:00 - 20:00
      int duration = 1 + rnd.nextInt(2);       // 1 or 2 hours

      LocalDateTime start = today.plusDays(dayOffset).atTime(hour, 0);
      b.setStartTime(start);
      b.setEndTime(start.plusHours(duration));

      // Mostly CONFIRMED, some PENDING, some CANCELLED.
      double s = rnd.nextDouble();
      if (s < 0.7)      b.setStatus(BookingStatus.CONFIRMED);
      else if (s < 0.9) b.setStatus(BookingStatus.PENDING);
      else              b.setStatus(BookingStatus.CANCELLED);

      b.setTotalPrice(BigDecimal.valueOf(10).multiply(BigDecimal.valueOf(duration)));
      b.setPaymentStatus(s < 0.7 ? PaymentStatus.PAID : PaymentStatus.PENDING);
      b.setPaymentMethod(PaymentMethod.SPLIT);

      em.persist(b);
      count++;
      if (count % 1000 == 0) {
        em.flush();
        em.clear();
      }
    }
    em.flush();
    em.clear();
    return count;
  }

  // --- Matches (manual entries, no booking link) -----------------------------

  private int seedMatches(List<User> users, List<Sports> sports) {
    int count = 0;
    for (int i = 0; i < sizes.matches; i++) {
      Sports sport = sports.get(rnd.nextInt(sports.size()));
      User creator = users.get(rnd.nextInt(users.size()));

      Match m = new Match();
      m.setSport(sport);
      m.setCreatedByUser(creator);
      m.setSource(MatchSource.MANUAL_ENTRY);
      m.setVerificationStatus(rnd.nextDouble() < 0.8
          ? MatchVerificationStatus.CONFIRMED
          : MatchVerificationStatus.PENDING);
      m.setMatchDate(LocalDateTime.now().minusDays(rnd.nextInt(365)));
      m.setScore("6-" + (1 + rnd.nextInt(6)) + ", 6-" + (1 + rnd.nextInt(6)));
      m.setStatus("FULL");

      // 2 participants (singles) or 4 (doubles) — randomize which.
      int playersPerTeam = rnd.nextDouble() < 0.7 ? 1 : 2;
      boolean teamAWins = rnd.nextBoolean();
      m.setWinningTeam(teamAWins ? "TEAM_A" : "TEAM_B");

      em.persist(m);

      // Add creator as TEAM_A host
      em.persist(participant(m, creator, "TEAM_A", true));

      // Random TEAM_A teammates
      Set<UUID> usedIds = new HashSet<>();
      usedIds.add(creator.getId());
      for (int j = 1; j < playersPerTeam; j++) {
        User u = pickUnique(users, usedIds);
        if (u != null) em.persist(participant(m, u, "TEAM_A", false));
      }
      // Random TEAM_B players
      for (int j = 0; j < playersPerTeam; j++) {
        User u = pickUnique(users, usedIds);
        if (u != null) em.persist(participant(m, u, "TEAM_B", false));
      }

      count++;
      if (count % 500 == 0) {
        em.flush();
        em.clear();
      }
    }
    em.flush();
    em.clear();
    return count;
  }

  private Participants participant(Match m, User u, String team, boolean isHost) {
    ParticipantsId pk = new ParticipantsId(m.getId(), u.getId());
    Participants p = new Participants();
    p.setId(pk);
    p.setMatch(m);
    p.setUser(u);
    p.setTeamName(team);
    p.setStatus(ParticipationStatus.ACCEPTED);
    p.setHost(isHost);
    return p;
  }

  private User pickUnique(List<User> users, Set<UUID> used) {
    for (int tries = 0; tries < 5; tries++) {
      User u = users.get(rnd.nextInt(users.size()));
      if (used.add(u.getId())) return u;
    }
    return null;
  }

  // --- Friendships -----------------------------------------------------------

  private int seedFriendships(List<User> users) {
    int target = users.size() * 3; // avg 3 friend edges per user
    int count = 0;
    Set<String> pairs = new HashSet<>();
    List<Friendship> batch = new ArrayList<>(1000);
    int safety = 0;
    while (count < target && safety++ < target * 4) {
      User a = users.get(rnd.nextInt(users.size()));
      User b = users.get(rnd.nextInt(users.size()));
      if (a.getId().equals(b.getId())) continue;
      String key = a.getId().compareTo(b.getId()) < 0
          ? a.getId() + ":" + b.getId()
          : b.getId() + ":" + a.getId();
      if (!pairs.add(key)) continue;

      Friendship f = new Friendship();
      f.setRequester(a);
      f.setReceiver(b);
      f.setStatus(rnd.nextDouble() < 0.8
          ? FriendshipStatus.ACCEPTED
          : FriendshipStatus.PENDING);
      f.setCreatedAt(LocalDateTime.now().minusDays(rnd.nextInt(180)));
      batch.add(f);
      count++;
      if (batch.size() >= 1000) {
        friendshipRepository.saveAll(batch);
        batch.clear();
        em.flush();
        em.clear();
      }
    }
    if (!batch.isEmpty()) friendshipRepository.saveAll(batch);
    return count;
  }

  // --- Size profile ----------------------------------------------------------

  private enum Sizes {
    SMALL  (100,  50,  500,    300),
    MEDIUM (1000, 300, 10000,  5000),
    LARGE  (5000, 1000, 100000, 30000);

    final int users, venues, bookings, matches;

    Sizes(int u, int v, int b, int m) {
      this.users = u; this.venues = v; this.bookings = b; this.matches = m;
    }

    static Sizes fromName(String name) {
      try {
        return Sizes.valueOf(name.trim().toUpperCase());
      } catch (Exception e) {
        return SMALL;
      }
    }
  }
}
