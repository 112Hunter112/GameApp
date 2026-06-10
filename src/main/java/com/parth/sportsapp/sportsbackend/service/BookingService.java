package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.BookingRequest;
import com.parth.sportsapp.sportsbackend.dto.BookingResponse;
import com.parth.sportsapp.sportsbackend.dto.CourtAvailabilityResponse;
import com.parth.sportsapp.sportsbackend.dto.TimeSlotDto;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.mapper.BookingMapper;
import com.parth.sportsapp.sportsbackend.model.*;
import com.parth.sportsapp.sportsbackend.repository.BookingRepository;
import com.parth.sportsapp.sportsbackend.repository.CourtRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Court booking engine.
 *
 * Booking lifecycle (inspired by Airbnb's request/instant-book model):
 *   create -> CONFIRMED (venue has instant book)  or  PENDING (request-to-book)
 *   owner: PENDING -> CONFIRMED | DECLINED
 *   either side: PENDING/CONFIRMED -> CANCELLED   (before start, player bound by cutoff)
 *   time:   CONFIRMED -> COMPLETED                (lazily, once endTime passes)
 *
 * Double-booking is prevented by taking a pessimistic lock on the court row
 * before the conflict check, so concurrent create() calls serialize.
 */
@Service
@Transactional
public class BookingService {

  @Autowired private BookingRepository bookingRepository;
  @Autowired private CourtRepository courtRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private BookingPolicyService bookingPolicyService;
  @Autowired private BookingMapper bookingMapper;
  @Autowired private NotificationService notificationService;

  // ===========================================================================
  // AVAILABILITY
  // ===========================================================================

  @Transactional(readOnly = true)
  public CourtAvailabilityResponse getAvailability(UUID courtId, LocalDate date) {
    Courts court = courtRepository.findById(courtId)
        .orElseThrow(() -> new NotFoundException("Court not found"));
    if (!court.isActive()) {
      throw new NotFoundException("Court not found");
    }

    BookingPolicy policy = bookingPolicyService.getEffectivePolicy(court.getVenue());

    LocalDate today = LocalDate.now();
    if (date.isBefore(today) || date.isAfter(today.plusDays(policy.getAdvanceBookingDays()))) {
      throw new BadRequestException(
          "Date must be between today and " + policy.getAdvanceBookingDays() + " days from now");
    }

    CourtAvailabilityResponse response = new CourtAvailabilityResponse();
    response.setCourtId(courtId);
    response.setDate(date);
    response.setSlotIncrementMinutes(policy.getSlotIncrementMinutes());
    response.setMinBookingMinutes(policy.getMinBookingMinutes());
    response.setMaxBookingMinutes(policy.getMaxBookingMinutes());
    response.setAdvanceBookingDays(policy.getAdvanceBookingDays());
    response.setAutoConfirm(policy.isAutoConfirm());
    response.setHourlyRate(court.getHourlyRate());

    LocalTime[] hours = openHoursFor(policy, date.getDayOfWeek());
    if (hours == null) {
      response.setClosed(true);
      response.setSlots(List.of());
      return response;
    }
    LocalTime open = hours[0];
    LocalTime close = hours[1];
    response.setClosed(false);
    response.setOpenTime(open.toString());
    response.setCloseTime(close.toString());

    // Existing slot-holding bookings that day
    LocalDateTime dayStart = date.atTime(open);
    LocalDateTime dayEnd = date.atTime(close);
    List<Booking> active = bookingRepository.findActiveInWindow(courtId, dayStart, dayEnd);

    // Earliest bookable instant (now + notice), so today's past slots show unavailable
    LocalDateTime earliestStart = LocalDateTime.now().plusMinutes(policy.getMinNoticeMinutes());

    List<TimeSlotDto> slots = new ArrayList<>();
    int inc = policy.getSlotIncrementMinutes();
    for (LocalDateTime s = dayStart; !s.plusMinutes(inc).isAfter(dayEnd); s = s.plusMinutes(inc)) {
      LocalDateTime e = s.plusMinutes(inc);
      boolean free = !s.isBefore(earliestStart) && !overlapsAny(active, s, e);
      slots.add(new TimeSlotDto(s, e, free));
    }
    response.setSlots(slots);
    return response;
  }

  // ===========================================================================
  // CREATE
  // ===========================================================================

  public BookingResponse createBooking(UUID playerId, BookingRequest request) {
    User player = userRepository.findById(playerId)
        .orElseThrow(() -> new NotFoundException("User not found"));

    // Lock the court row first: from here to commit, no other transaction can
    // run the conflict check for this court.
    Courts court = courtRepository.findByIdForUpdate(request.getCourtId())
        .orElseThrow(() -> new NotFoundException("Court not found"));
    if (!court.isActive()) {
      throw new NotFoundException("Court not found");
    }

    Venue venue = court.getVenue();
    BookingPolicy policy = bookingPolicyService.getEffectivePolicy(venue);

    LocalDateTime start = request.getStartTime();
    LocalDateTime end = request.getEndTime();
    validateRequestedWindow(policy, start, end);

    if (venue.getOwner().getId().equals(playerId)) {
      throw new BadRequestException("You cannot book your own venue");
    }

    if (bookingRepository.countConflicts(court.getId(), start, end) > 0) {
      throw new BadRequestException("That time is no longer available. Please pick another slot.");
    }

    Booking booking = new Booking();
    booking.setUser(player);
    booking.setCourt(court);
    booking.setStartTime(start);
    booking.setEndTime(end);
    booking.setNotes(request.getNotes());
    booking.setTotalPrice(price(court.getHourlyRate(), start, end));
    booking.setPaymentMethod(PaymentMethod.CASH);   // pay at venue (Stripe later)
    booking.setPaymentStatus(PaymentStatus.PENDING);

    if (policy.isAutoConfirm()) {
      booking.setStatus(BookingStatus.CONFIRMED);
      booking.setConfirmedAt(LocalDateTime.now());
    } else {
      booking.setStatus(BookingStatus.PENDING);
    }

    Booking saved = bookingRepository.save(booking);

    notificationService.sendBookingCreated(venue.getOwner(), player, saved);

    return bookingMapper.toResponse(saved, false, canPlayerCancel(saved, policy));
  }

  // ===========================================================================
  // PLAYER VIEWS
  // ===========================================================================

  @Transactional(readOnly = true)
  public Page<BookingResponse> getMyUpcoming(UUID playerId, Pageable pageable) {
    LocalDateTime now = LocalDateTime.now();
    return bookingRepository
        .findByUser_IdAndEndTimeGreaterThanEqualAndStatusInOrderByStartTimeAsc(
            playerId, now, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED), pageable)
        .map(b -> toPlayerResponse(b));
  }

  @Transactional(readOnly = true)
  public Page<BookingResponse> getMyPast(UUID playerId, Pageable pageable) {
    return bookingRepository.findPastForUser(playerId, LocalDateTime.now(), pageable)
        .map(b -> bookingMapper.toResponse(completeIfOver(b), false, false));
  }

  @Transactional(readOnly = true)
  public BookingResponse getBooking(UUID bookingId, UUID requesterId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new NotFoundException("Booking not found"));

    boolean isPlayer = booking.getUser().getId().equals(requesterId);
    boolean isOwner = booking.getCourt().getVenue().getOwner().getId().equals(requesterId);
    if (!isPlayer && !isOwner) {
      throw new ForbiddenException("You cannot view this booking");
    }

    Booking b = completeIfOver(booking);
    if (isOwner) {
      return bookingMapper.toResponse(b, true, canOwnerCancel(b));
    }
    BookingPolicy policy = bookingPolicyService.getEffectivePolicy(b.getCourt().getVenue());
    return bookingMapper.toResponse(b, false, canPlayerCancel(b, policy));
  }

  // ===========================================================================
  // OWNER VIEWS & ACTIONS
  // ===========================================================================

  @Transactional(readOnly = true)
  public Page<BookingResponse> getOwnerBookings(UUID ownerId, UUID venueId, LocalDate date,
                                                BookingStatus status, Pageable pageable) {
    LocalDateTime dayStart = date != null ? date.atStartOfDay() : null;
    LocalDateTime dayEnd = date != null ? date.plusDays(1).atStartOfDay() : null;
    return bookingRepository.findForOwner(ownerId, venueId, status, dayStart, dayEnd, pageable)
        .map(b -> bookingMapper.toResponse(completeIfOver(b), true, canOwnerCancel(b)));
  }

  @Transactional(readOnly = true)
  public long getOwnerPendingCount(UUID ownerId) {
    return bookingRepository.countPendingForOwner(ownerId, LocalDateTime.now());
  }

  public BookingResponse confirmBooking(UUID bookingId, UUID ownerId) {
    Booking booking = ownedBooking(bookingId, ownerId);

    if (booking.getStatus() != BookingStatus.PENDING) {
      throw new BadRequestException("Only pending bookings can be confirmed");
    }
    if (booking.getStartTime().isBefore(LocalDateTime.now())) {
      throw new BadRequestException("This booking's start time has already passed");
    }

    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setConfirmedAt(LocalDateTime.now());
    Booking saved = bookingRepository.save(booking);

    notificationService.sendBookingConfirmed(saved.getUser(), saved);
    return bookingMapper.toResponse(saved, true, canOwnerCancel(saved));
  }

  public BookingResponse declineBooking(UUID bookingId, UUID ownerId, String reason) {
    Booking booking = ownedBooking(bookingId, ownerId);

    if (booking.getStatus() != BookingStatus.PENDING) {
      throw new BadRequestException("Only pending bookings can be declined");
    }

    booking.setStatus(BookingStatus.DECLINED);
    booking.setCancelledAt(LocalDateTime.now());
    booking.setCancelledBy(CancellationActor.OWNER);
    booking.setCancellationReason(trimReason(reason));
    Booking saved = bookingRepository.save(booking);

    notificationService.sendBookingDeclined(saved.getUser(), saved);
    return bookingMapper.toResponse(saved, true, false);
  }

  // ===========================================================================
  // CANCEL (either side)
  // ===========================================================================

  public BookingResponse cancelBooking(UUID bookingId, UUID requesterId, String reason) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new NotFoundException("Booking not found"));

    boolean isPlayer = booking.getUser().getId().equals(requesterId);
    boolean isOwner = booking.getCourt().getVenue().getOwner().getId().equals(requesterId);
    if (!isPlayer && !isOwner) {
      throw new ForbiddenException("You cannot cancel this booking");
    }

    if (!booking.getStatus().blocksSlot()) {
      throw new BadRequestException("This booking is already " +
          booking.getStatus().name().toLowerCase());
    }
    if (booking.getStartTime().isBefore(LocalDateTime.now())) {
      throw new BadRequestException("Bookings that have started cannot be cancelled");
    }

    if (isPlayer && !isOwner) {
      BookingPolicy policy = bookingPolicyService.getEffectivePolicy(booking.getCourt().getVenue());
      LocalDateTime cutoff = booking.getStartTime().minusMinutes(policy.getCancellationCutoffMinutes());
      if (LocalDateTime.now().isAfter(cutoff)) {
        throw new BadRequestException("Cancellations close " +
            formatMinutes(policy.getCancellationCutoffMinutes()) + " before the start time");
      }
    }

    booking.setStatus(BookingStatus.CANCELLED);
    booking.setCancelledAt(LocalDateTime.now());
    booking.setCancelledBy(isPlayer ? CancellationActor.PLAYER : CancellationActor.OWNER);
    booking.setCancellationReason(trimReason(reason));
    Booking saved = bookingRepository.save(booking);

    if (isPlayer) {
      notificationService.sendBookingCancelledByPlayer(
          saved.getCourt().getVenue().getOwner(), saved.getUser(), saved);
    } else {
      notificationService.sendBookingCancelledByVenue(saved.getUser(), saved);
    }

    return bookingMapper.toResponse(saved, isOwner, false);
  }

  // ===========================================================================
  // Helpers
  // ===========================================================================

  /** Opening/closing time for the day, or null when closed. */
  private LocalTime[] openHoursFor(BookingPolicy policy, DayOfWeek day) {
    if (policy.getOpeningHours() == null) return null;
    String range = policy.getOpeningHours().get(day.name());
    if (range == null || range.length() != 11) return null;
    try {
      LocalTime open = LocalTime.parse(range.substring(0, 5));
      LocalTime close = LocalTime.parse(range.substring(6));
      return close.isAfter(open) ? new LocalTime[]{open, close} : null;
    } catch (Exception e) {
      return null;
    }
  }

  private void validateRequestedWindow(BookingPolicy policy, LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null || !end.isAfter(start)) {
      throw new BadRequestException("End time must be after start time");
    }
    if (!start.toLocalDate().equals(end.toLocalDate())) {
      throw new BadRequestException("A booking must start and end on the same day");
    }

    long minutes = Duration.between(start, end).toMinutes();
    int inc = policy.getSlotIncrementMinutes();

    if (minutes < policy.getMinBookingMinutes() || minutes > policy.getMaxBookingMinutes()) {
      throw new BadRequestException("Booking length must be between " +
          formatMinutes(policy.getMinBookingMinutes()) + " and " +
          formatMinutes(policy.getMaxBookingMinutes()));
    }
    if (minutes % inc != 0 || (start.getMinute() % inc != 0) || start.getSecond() != 0) {
      throw new BadRequestException("Times must align to the venue's " + inc + "-minute grid");
    }

    LocalDateTime earliest = LocalDateTime.now().plusMinutes(policy.getMinNoticeMinutes());
    if (start.isBefore(earliest)) {
      throw new BadRequestException("Bookings need at least " +
          formatMinutes(policy.getMinNoticeMinutes()) + " notice");
    }
    LocalDate lastDay = LocalDate.now().plusDays(policy.getAdvanceBookingDays());
    if (start.toLocalDate().isAfter(lastDay)) {
      throw new BadRequestException("This venue only accepts bookings up to " +
          policy.getAdvanceBookingDays() + " days ahead");
    }

    LocalTime[] hours = openHoursFor(policy, start.getDayOfWeek());
    if (hours == null) {
      throw new BadRequestException("The venue is closed on " + start.getDayOfWeek());
    }
    if (start.toLocalTime().isBefore(hours[0]) || end.toLocalTime().isAfter(hours[1])) {
      throw new BadRequestException("The venue is open " + hours[0] + "–" + hours[1] + " that day");
    }
  }

  /** hourlyRate × minutes ÷ 60, money-rounded. Falls back to 0 for rate-less courts. */
  public static BigDecimal price(BigDecimal hourlyRate, LocalDateTime start, LocalDateTime end) {
    if (hourlyRate == null) return BigDecimal.ZERO;
    long minutes = Duration.between(start, end).toMinutes();
    return hourlyRate.multiply(BigDecimal.valueOf(minutes))
        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
  }

  private boolean overlapsAny(List<Booking> bookings, LocalDateTime s, LocalDateTime e) {
    for (Booking b : bookings) {
      if (b.getStartTime().isBefore(e) && b.getEndTime().isAfter(s)) return true;
    }
    return false;
  }

  private Booking ownedBooking(UUID bookingId, UUID ownerId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new NotFoundException("Booking not found"));
    if (!booking.getCourt().getVenue().getOwner().getId().equals(ownerId)) {
      throw new ForbiddenException("You do not manage this booking's venue");
    }
    return booking;
  }

  /**
   * Present finished CONFIRMED bookings as COMPLETED. The change is in-memory
   * only (these run inside read-only transactions, which never flush), so the
   * stored status stays CONFIRMED — harmless, because every read recomputes
   * this and a finished booking can't be confirmed, declined or cancelled.
   */
  private Booking completeIfOver(Booking b) {
    if (b.getStatus() == BookingStatus.CONFIRMED && b.getEndTime().isBefore(LocalDateTime.now())) {
      b.setStatus(BookingStatus.COMPLETED);
    }
    return b;
  }

  private BookingResponse toPlayerResponse(Booking b) {
    BookingPolicy policy = bookingPolicyService.getEffectivePolicy(b.getCourt().getVenue());
    return bookingMapper.toResponse(b, false, canPlayerCancel(b, policy));
  }

  private boolean canPlayerCancel(Booking b, BookingPolicy policy) {
    return b.getStatus().blocksSlot()
        && LocalDateTime.now().isBefore(
            b.getStartTime().minusMinutes(policy.getCancellationCutoffMinutes()));
  }

  private boolean canOwnerCancel(Booking b) {
    return b.getStatus().blocksSlot() && b.getStartTime().isAfter(LocalDateTime.now());
  }

  private String trimReason(String reason) {
    if (reason == null) return null;
    String r = reason.trim();
    return r.isEmpty() ? null : (r.length() > 500 ? r.substring(0, 500) : r);
  }

  private String formatMinutes(int minutes) {
    if (minutes < 60) return minutes + " minutes";
    if (minutes % 60 == 0) return (minutes / 60) + (minutes == 60 ? " hour" : " hours");
    return (minutes / 60.0) + " hours";
  }
}
