package com.parth.sportsapp.sportsbackend;

import com.parth.sportsapp.sportsbackend.dto.BookingRequest;
import com.parth.sportsapp.sportsbackend.dto.BookingResponse;
import com.parth.sportsapp.sportsbackend.dto.CourtAvailabilityResponse;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.mapper.BookingMapper;
import com.parth.sportsapp.sportsbackend.model.*;
import com.parth.sportsapp.sportsbackend.repository.BookingRepository;
import com.parth.sportsapp.sportsbackend.repository.CourtRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import com.parth.sportsapp.sportsbackend.service.BookingPolicyService;
import com.parth.sportsapp.sportsbackend.service.BookingService;
import com.parth.sportsapp.sportsbackend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

  @InjectMocks
  private BookingService bookingService;

  @Mock private BookingRepository bookingRepository;
  @Mock private CourtRepository courtRepository;
  @Mock private UserRepository userRepository;
  @Mock private BookingPolicyService bookingPolicyService;
  @Mock private NotificationService notificationService;
  @Mock private BookingMapper bookingMapper;

  private User player;
  private User owner;
  private Venue venue;
  private Courts court;
  private BookingPolicy policy;

  @BeforeEach
  public void setUp() {
    player = new User();
    player.setId(UUID.randomUUID());

    owner = new User();
    owner.setId(UUID.randomUUID());

    venue = new Venue();
    venue.setId(UUID.randomUUID());
    venue.setOwner(owner);
    venue.setName("Test Venue");

    Sports sport = new Sports();
    sport.setSportName("Tennis");

    court = new Courts();
    court.setId(UUID.randomUUID());
    court.setVenue(venue);
    court.setSports(sport);
    court.setCourtNumber("Court 1");
    court.setHourlyRate(new BigDecimal("20.00"));
    court.setActive(true);

    // 30-min grid, 1h–3h sessions, open every day 08:00–22:00, instant book
    policy = BookingPolicy.defaults(venue);
    policy.setSlotIncrementMinutes(30);
  }

  /** Tomorrow at the given time — always inside notice + advance windows. */
  private LocalDateTime tomorrowAt(int hour, int minute) {
    return LocalDate.now().plusDays(1).atTime(LocalTime.of(hour, minute));
  }

  // ===========================================================================
  // PRICING
  // ===========================================================================

  @Test
  public void price_proRatesHalfHours() {
    // 1.5h at £20/h = £30
    assertEquals(new BigDecimal("30.00"),
        BookingService.price(new BigDecimal("20.00"), tomorrowAt(10, 0), tomorrowAt(11, 30)));
    // 2.5h at £18/h = £45
    assertEquals(new BigDecimal("45.00"),
        BookingService.price(new BigDecimal("18.00"), tomorrowAt(10, 0), tomorrowAt(12, 30)));
    // null rate -> 0
    assertEquals(BigDecimal.ZERO,
        BookingService.price(null, tomorrowAt(10, 0), tomorrowAt(11, 0)));
  }

  // ===========================================================================
  // CREATE
  // ===========================================================================

  private BookingRequest request(LocalDateTime start, LocalDateTime end) {
    BookingRequest r = new BookingRequest();
    r.setCourtId(court.getId());
    r.setStartTime(start);
    r.setEndTime(end);
    return r;
  }

  private void stubHappyPath() {
    when(userRepository.findById(player.getId())).thenReturn(Optional.of(player));
    when(courtRepository.findByIdForUpdate(court.getId())).thenReturn(Optional.of(court));
    when(bookingPolicyService.getEffectivePolicy(venue)).thenReturn(policy);
  }

  @Test
  public void createBooking_instantBook_confirmsAndNotifiesOwner() {
    stubHappyPath();
    when(bookingRepository.countConflicts(eq(court.getId()), any(), any())).thenReturn(0L);
    when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    when(bookingMapper.toResponse(any(), anyBoolean(), anyBoolean())).thenReturn(new BookingResponse());

    bookingService.createBooking(player.getId(), request(tomorrowAt(10, 0), tomorrowAt(11, 30)));

    verify(bookingRepository).save(argThat(b ->
        b.getStatus() == BookingStatus.CONFIRMED
            && b.getTotalPrice().compareTo(new BigDecimal("30.00")) == 0
            && b.getConfirmedAt() != null));
    verify(notificationService).sendBookingCreated(eq(owner), eq(player), any(Booking.class));
  }

  @Test
  public void createBooking_requestToBook_staysPending() {
    policy.setAutoConfirm(false);
    stubHappyPath();
    when(bookingRepository.countConflicts(eq(court.getId()), any(), any())).thenReturn(0L);
    when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    when(bookingMapper.toResponse(any(), anyBoolean(), anyBoolean())).thenReturn(new BookingResponse());

    bookingService.createBooking(player.getId(), request(tomorrowAt(10, 0), tomorrowAt(11, 0)));

    verify(bookingRepository).save(argThat(b -> b.getStatus() == BookingStatus.PENDING));
  }

  @Test
  public void createBooking_conflict_throws() {
    stubHappyPath();
    when(bookingRepository.countConflicts(eq(court.getId()), any(), any())).thenReturn(1L);

    assertThrows(BadRequestException.class, () ->
        bookingService.createBooking(player.getId(), request(tomorrowAt(10, 0), tomorrowAt(11, 0))));
    verify(bookingRepository, never()).save(any());
  }

  @Test
  public void createBooking_offGridStart_throws() {
    policy.setSlotIncrementMinutes(60); // hourly grid: 10:30 start is invalid
    stubHappyPath();

    assertThrows(BadRequestException.class, () ->
        bookingService.createBooking(player.getId(), request(tomorrowAt(10, 30), tomorrowAt(11, 30))));
  }

  @Test
  public void createBooking_tooShort_throws() {
    stubHappyPath(); // min is 60 minutes

    assertThrows(BadRequestException.class, () ->
        bookingService.createBooking(player.getId(), request(tomorrowAt(10, 0), tomorrowAt(10, 30))));
  }

  @Test
  public void createBooking_outsideOpeningHours_throws() {
    stubHappyPath(); // open 08:00–22:00

    assertThrows(BadRequestException.class, () ->
        bookingService.createBooking(player.getId(), request(tomorrowAt(21, 30), tomorrowAt(22, 30))));
  }

  @Test
  public void createBooking_closedDay_throws() {
    policy.setOpeningHours(Map.of()); // closed all week
    stubHappyPath();

    assertThrows(BadRequestException.class, () ->
        bookingService.createBooking(player.getId(), request(tomorrowAt(10, 0), tomorrowAt(11, 0))));
  }

  @Test
  public void createBooking_ownVenue_throws() {
    when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
    when(courtRepository.findByIdForUpdate(court.getId())).thenReturn(Optional.of(court));
    when(bookingPolicyService.getEffectivePolicy(venue)).thenReturn(policy);

    assertThrows(BadRequestException.class, () ->
        bookingService.createBooking(owner.getId(), request(tomorrowAt(10, 0), tomorrowAt(11, 0))));
  }

  // ===========================================================================
  // OWNER ACTIONS
  // ===========================================================================

  private Booking pendingBooking() {
    Booking b = new Booking();
    b.setId(UUID.randomUUID());
    b.setUser(player);
    b.setCourt(court);
    b.setStartTime(tomorrowAt(10, 0));
    b.setEndTime(tomorrowAt(11, 0));
    b.setStatus(BookingStatus.PENDING);
    return b;
  }

  @Test
  public void confirmBooking_byOwner_confirmsAndNotifiesPlayer() {
    Booking b = pendingBooking();
    when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));
    when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    when(bookingMapper.toResponse(any(), anyBoolean(), anyBoolean())).thenReturn(new BookingResponse());

    bookingService.confirmBooking(b.getId(), owner.getId());

    assertEquals(BookingStatus.CONFIRMED, b.getStatus());
    verify(notificationService).sendBookingConfirmed(eq(player), any(Booking.class));
  }

  @Test
  public void confirmBooking_byStranger_forbidden() {
    Booking b = pendingBooking();
    when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));

    assertThrows(ForbiddenException.class, () ->
        bookingService.confirmBooking(b.getId(), UUID.randomUUID()));
  }

  @Test
  public void declineBooking_freesSlotAndRecordsReason() {
    Booking b = pendingBooking();
    when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));
    when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    when(bookingMapper.toResponse(any(), anyBoolean(), anyBoolean())).thenReturn(new BookingResponse());

    bookingService.declineBooking(b.getId(), owner.getId(), "Maintenance that day");

    assertEquals(BookingStatus.DECLINED, b.getStatus());
    assertEquals(CancellationActor.OWNER, b.getCancelledBy());
    assertEquals("Maintenance that day", b.getCancellationReason());
    verify(notificationService).sendBookingDeclined(eq(player), any(Booking.class));
  }

  // ===========================================================================
  // CANCEL
  // ===========================================================================

  @Test
  public void cancelBooking_byPlayer_withinCutoff_works() {
    Booking b = pendingBooking();
    b.setStatus(BookingStatus.CONFIRMED);
    when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));
    when(bookingPolicyService.getEffectivePolicy(venue)).thenReturn(policy);
    when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    when(bookingMapper.toResponse(any(), anyBoolean(), anyBoolean())).thenReturn(new BookingResponse());

    bookingService.cancelBooking(b.getId(), player.getId(), null);

    assertEquals(BookingStatus.CANCELLED, b.getStatus());
    assertEquals(CancellationActor.PLAYER, b.getCancelledBy());
    verify(notificationService).sendBookingCancelledByPlayer(eq(owner), eq(player), any(Booking.class));
  }

  @Test
  public void cancelBooking_byPlayer_pastCutoff_throws() {
    Booking b = pendingBooking();
    b.setStatus(BookingStatus.CONFIRMED);
    // Booking starts in 1 hour; cutoff is 2 hours -> too late to cancel
    b.setStartTime(LocalDateTime.now().plusMinutes(60));
    b.setEndTime(LocalDateTime.now().plusMinutes(120));
    when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));
    when(bookingPolicyService.getEffectivePolicy(venue)).thenReturn(policy);

    assertThrows(BadRequestException.class, () ->
        bookingService.cancelBooking(b.getId(), player.getId(), null));
  }

  @Test
  public void cancelBooking_byOwner_ignoresCutoffAndNotifiesPlayer() {
    Booking b = pendingBooking();
    b.setStatus(BookingStatus.CONFIRMED);
    b.setStartTime(LocalDateTime.now().plusMinutes(30)); // inside player cutoff
    b.setEndTime(LocalDateTime.now().plusMinutes(90));
    when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));
    when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    when(bookingMapper.toResponse(any(), anyBoolean(), anyBoolean())).thenReturn(new BookingResponse());

    bookingService.cancelBooking(b.getId(), owner.getId(), "Flooded court");

    assertEquals(BookingStatus.CANCELLED, b.getStatus());
    assertEquals(CancellationActor.OWNER, b.getCancelledBy());
    verify(notificationService).sendBookingCancelledByVenue(eq(player), any(Booking.class));
  }

  @Test
  public void cancelBooking_byStranger_forbidden() {
    Booking b = pendingBooking();
    when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));

    assertThrows(ForbiddenException.class, () ->
        bookingService.cancelBooking(b.getId(), UUID.randomUUID(), null));
  }

  // ===========================================================================
  // AVAILABILITY
  // ===========================================================================

  @Test
  public void getAvailability_marksBookedSlotsUnavailable() {
    when(courtRepository.findById(court.getId())).thenReturn(Optional.of(court));
    when(bookingPolicyService.getEffectivePolicy(venue)).thenReturn(policy);

    LocalDate date = LocalDate.now().plusDays(1);
    Booking existing = pendingBooking();
    existing.setStartTime(date.atTime(10, 0));
    existing.setEndTime(date.atTime(11, 30));
    when(bookingRepository.findActiveInWindow(eq(court.getId()), any(), any()))
        .thenReturn(List.of(existing));

    CourtAvailabilityResponse availability = bookingService.getAvailability(court.getId(), date);

    assertFalse(availability.isClosed());
    assertEquals(30, availability.getSlotIncrementMinutes());
    // 08:00–22:00 on a 30-min grid = 28 slots
    assertEquals(28, availability.getSlots().size());
    // 10:00, 10:30, 11:00 blocked; 09:30 and 11:30 free
    assertFalse(slotAt(availability, date.atTime(10, 0)));
    assertFalse(slotAt(availability, date.atTime(10, 30)));
    assertFalse(slotAt(availability, date.atTime(11, 0)));
    assertTrue(slotAt(availability, date.atTime(9, 30)));
    assertTrue(slotAt(availability, date.atTime(11, 30)));
  }

  @Test
  public void getAvailability_closedDay_returnsClosed() {
    policy.setOpeningHours(Map.of()); // closed every day
    when(courtRepository.findById(court.getId())).thenReturn(Optional.of(court));
    when(bookingPolicyService.getEffectivePolicy(venue)).thenReturn(policy);

    CourtAvailabilityResponse availability =
        bookingService.getAvailability(court.getId(), LocalDate.now().plusDays(1));

    assertTrue(availability.isClosed());
    assertTrue(availability.getSlots().isEmpty());
  }

  @Test
  public void getAvailability_dateBeyondWindow_throws() {
    when(courtRepository.findById(court.getId())).thenReturn(Optional.of(court));
    when(bookingPolicyService.getEffectivePolicy(venue)).thenReturn(policy);

    assertThrows(BadRequestException.class, () ->
        bookingService.getAvailability(court.getId(),
            LocalDate.now().plusDays(policy.getAdvanceBookingDays() + 1)));
  }

  private boolean slotAt(CourtAvailabilityResponse availability, LocalDateTime start) {
    return availability.getSlots().stream()
        .filter(s -> s.getStartTime().equals(start))
        .findFirst()
        .orElseThrow()
        .isAvailable();
  }
}
