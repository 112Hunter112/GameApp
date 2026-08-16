package com.parth.sportsapp.sportsbackend;

import com.parth.sportsapp.sportsbackend.dto.BookingRequest;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.model.Booking;
import com.parth.sportsapp.sportsbackend.model.BookingStatus;
import com.parth.sportsapp.sportsbackend.model.CourtBlock;
import com.parth.sportsapp.sportsbackend.model.Courts;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserRole;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.repository.BookingRepository;
import com.parth.sportsapp.sportsbackend.repository.CourtBlockRepository;
import com.parth.sportsapp.sportsbackend.repository.CourtRepository;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import com.parth.sportsapp.sportsbackend.repository.VenueRepository;
import com.parth.sportsapp.sportsbackend.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contention and conflict-semantics tests for the booking engine against a
 * real Postgres (Testcontainers — requires Docker, runs under `mvn verify`).
 *
 * A popular court's prime-time slot is this schema's hot partition: every
 * booking attempt for it serializes on the court row via
 * CourtRepository#findByIdForUpdate. These tests prove the serialization
 * actually holds under concurrency (no double-booking), that it is scoped to
 * one court (other courts don't queue behind it), and that the overlap window
 * semantics [start, end) are exact at the boundaries.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BookingConflictIT {

  @Container
  static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
      DockerImageName.parse("postgis/postgis:15-3.4")
          .asCompatibleSubstituteFor("postgres"))
      .withInitScript("init-postgis.sql");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGIS::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGIS::getUsername);
    registry.add("spring.datasource.password", POSTGIS::getPassword);
  }

  // No real Redis in the test; the booking path doesn't exercise Redisson.
  @MockitoBean
  RedissonClient redissonClient;

  @Autowired BookingService bookingService;
  @Autowired BookingRepository bookingRepository;
  @Autowired CourtBlockRepository courtBlockRepository;
  @Autowired CourtRepository courtRepository;
  @Autowired VenueRepository venueRepository;
  @Autowired UserRepository userRepository;
  @Autowired SportsRepository sportsRepository;

  private User owner;
  private Venue venue;
  private Sports sports;

  /** Tomorrow 18:00–19:00 — inside default policy (grid, notice, hours, window). */
  private final LocalDateTime slotStart = LocalDate.now().plusDays(1).atTime(18, 0);
  private final LocalDateTime slotEnd = slotStart.plusHours(1);

  @BeforeEach
  void setUp() {
    owner = saveUser("owner");

    sports = new Sports();
    sports.setSportName("Tennis-" + System.nanoTime());
    sports.setMinPlayers(2);
    sports.setMaxPlayers(4);
    sports = sportsRepository.save(sports);

    venue = new Venue();
    venue.setName("Hot Venue " + System.nanoTime());
    venue.setAddress("1 Prime Time Ave");
    venue.setOwner(owner);
    venue.setOpeningHours(Map.of());
    venue.setAmenities(List.of());
    venue.setActive(true);
    venue = venueRepository.save(venue);
  }

  // --- the hot slot under concurrency ----------------------------------------

  @Test
  @Timeout(90)
  void hotSlotUnderConcurrencyBooksExactlyOnce() throws Exception {
    Courts court = saveCourt();
    int players = 6;
    List<User> contenders = new ArrayList<>();
    for (int i = 0; i < players; i++) {
      contenders.add(saveUser("player" + i));
    }

    // All threads fire createBooking for the SAME court+slot at the same instant.
    CyclicBarrier gate = new CyclicBarrier(players);
    List<Callable<Boolean>> attempts = contenders.stream()
        .map(p -> (Callable<Boolean>) () -> {
          gate.await();
          try {
            bookingService.createBooking(p.getId(), request(court, slotStart, slotEnd));
            return true;
          } catch (BadRequestException conflict) {
            return false; // lost the race — the only acceptable failure mode
          }
        })
        .toList();

    ExecutorService pool = Executors.newFixedThreadPool(players);
    long wins;
    try {
      List<Future<Boolean>> results = pool.invokeAll(attempts);
      wins = 0;
      for (Future<Boolean> r : results) {
        // Anything other than true/false (lock timeout, constraint blow-up,
        // deadlock) propagates here and fails the test loudly.
        if (r.get()) wins++;
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(wins).as("exactly one contender may win the slot").isEqualTo(1);
    assertThat(bookingRepository.countConflicts(court.getId(), slotStart, slotEnd))
        .as("exactly one slot-holding row may exist")
        .isEqualTo(1);
  }

  @Test
  @Timeout(90)
  void differentCourtsDoNotBlockEachOther() throws Exception {
    Courts courtA = saveCourt();
    Courts courtB = saveCourt();
    User playerA = saveUser("playerA");
    User playerB = saveUser("playerB");

    CyclicBarrier gate = new CyclicBarrier(2);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Boolean> a = pool.submit(() -> {
        gate.await();
        bookingService.createBooking(playerA.getId(), request(courtA, slotStart, slotEnd));
        return true;
      });
      Future<Boolean> b = pool.submit(() -> {
        gate.await();
        bookingService.createBooking(playerB.getId(), request(courtB, slotStart, slotEnd));
        return true;
      });

      // The court-row lock must be per court: same slot on two courts, both win.
      assertThat(a.get()).isTrue();
      assertThat(b.get()).isTrue();
    } finally {
      pool.shutdownNow();
    }
  }

  // --- overlap window semantics ------------------------------------------------

  @Test
  void backToBackBookingsShareABoundaryWithoutConflicting() {
    Courts court = saveCourt();
    User first = saveUser("first");
    User second = saveUser("second");

    bookingService.createBooking(first.getId(), request(court, slotStart, slotEnd));

    // [18:00,19:00) then [19:00,20:00): touching endpoints must NOT conflict —
    // this is the exact boundary of the s1 < e2 AND e1 > s2 predicate.
    bookingService.createBooking(second.getId(), request(court, slotEnd, slotEnd.plusHours(1)));

    assertThat(bookingRepository.countConflicts(court.getId(), slotStart, slotEnd.plusHours(1)))
        .isEqualTo(2);
  }

  @Test
  void straddlingBookingIsRejected() {
    Courts court = saveCourt();
    User first = saveUser("first");
    User second = saveUser("second");

    bookingService.createBooking(first.getId(), request(court, slotStart, slotEnd));

    // [18:00,20:00) straddles the existing [18:00,19:00).
    assertThatThrownBy(() ->
        bookingService.createBooking(second.getId(), request(court, slotStart, slotStart.plusHours(2))))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("no longer available");
  }

  @Test
  void cancelledBookingFreesTheSlotForRebooking() {
    Courts court = saveCourt();
    User first = saveUser("first");
    User second = saveUser("second");

    bookingService.createBooking(first.getId(), request(court, slotStart, slotEnd));

    Booking held = bookingRepository
        .findActiveInWindow(court.getId(), slotStart, slotEnd).get(0);
    held.setStatus(BookingStatus.CANCELLED);
    bookingRepository.save(held);

    // CANCELLED no longer blocks the slot, so a rebooking must go through.
    bookingService.createBooking(second.getId(), request(court, slotStart, slotEnd));

    assertThat(bookingRepository.countConflicts(court.getId(), slotStart, slotEnd)).isEqualTo(1);
  }

  @Test
  void ownerBlockRejectsOverlappingBooking() {
    Courts court = saveCourt();
    User player = saveUser("player");

    CourtBlock block = new CourtBlock();
    block.setCourt(court);
    block.setStartTime(slotStart);
    block.setEndTime(slotEnd);
    block.setReason("Resurfacing");
    courtBlockRepository.save(block);

    // Blocks must hold the slot exactly like a confirmed booking would.
    assertThatThrownBy(() ->
        bookingService.createBooking(player.getId(), request(court, slotStart, slotEnd)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("no longer available");
  }

  // --- helpers ---------------------------------------------------------------

  private BookingRequest request(Courts court, LocalDateTime start, LocalDateTime end) {
    BookingRequest r = new BookingRequest();
    r.setCourtId(court.getId());
    r.setStartTime(start);
    r.setEndTime(end);
    return r;
  }

  private Courts saveCourt() {
    Courts c = new Courts();
    c.setVenue(venue);
    c.setSports(sports);
    c.setCourtNumber("Court-" + System.nanoTime());
    c.setHourlyRate(new BigDecimal("20.00"));
    c.setActive(true);
    return courtRepository.save(c);
  }

  private User saveUser(String tag) {
    User u = new User();
    u.setFirstName(tag);
    u.setLastName("Test");
    u.setEmail(tag + "-" + System.nanoTime() + "@example.com");
    u.setPassword("placeholder");
    u.setRole(UserRole.USER);
    u.setJoiningDate(LocalDateTime.now());
    u.setUpdatedAt(LocalDateTime.now());
    u.setVerified(true);
    return userRepository.save(u);
  }
}
