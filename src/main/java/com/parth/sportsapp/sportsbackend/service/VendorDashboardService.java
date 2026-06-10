package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.VendorBookingEntry;
import com.parth.sportsapp.sportsbackend.dto.VendorDashboardResponse;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.Booking;
import com.parth.sportsapp.sportsbackend.model.BookingStatus;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.repository.BookingRepository;
import com.parth.sportsapp.sportsbackend.repository.VenueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Read-side operations for the venue owner's "booking manager" screens.
 *
 * <p>Every method takes the authenticated vendor's id (from the JWT) and
 * verifies it against {@code venue.owner} before returning anything —
 * a vendor can only ever see their own venues' data.</p>
 */
@Service
public class VendorDashboardService {

  /** Revenue counts these statuses (PENDING/CANCELLED/NO_SHOW excluded). */
  private static final EnumSet<BookingStatus> REVENUE_STATUSES =
      EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

  private static final int UPCOMING_LIMIT = 20;

  private final BookingRepository bookingRepository;
  private final VenueRepository venueRepository;

  public VendorDashboardService(BookingRepository bookingRepository,
                                VenueRepository venueRepository) {
    this.bookingRepository = bookingRepository;
    this.venueRepository = venueRepository;
  }

  // =====================================================================
  // Dashboard snapshot
  // =====================================================================

  @Transactional(readOnly = true)
  public VendorDashboardResponse getDashboard(UUID vendorId, UUID venueId) {
    Venue venue = requireOwnedVenue(vendorId, venueId);

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
    LocalDateTime startOfWeek  = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
    LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
    LocalDateTime thirtyDaysAgo = now.minusDays(30);

    VendorDashboardResponse res = new VendorDashboardResponse();
    res.setVenueId(venue.getId());
    res.setVenueName(venue.getName());

    res.setRevenueThisWeek(
        bookingRepository.sumRevenue(venueId, REVENUE_STATUSES, startOfWeek, now));
    res.setRevenueThisMonth(
        bookingRepository.sumRevenue(venueId, REVENUE_STATUSES, startOfMonth, now));

    res.setBookingsToday(
        bookingRepository.countByCourt_Venue_IdAndStatusAndStartTimeBetween(
            venueId, BookingStatus.CONFIRMED, startOfToday, startOfToday.plusDays(1)));
    res.setNoShowsLast30Days(
        bookingRepository.countByCourt_Venue_IdAndStatusAndStartTimeBetween(
            venueId, BookingStatus.NO_SHOW, thirtyDaysAgo, now));
    res.setCancellationsLast30Days(
        bookingRepository.countByCourt_Venue_IdAndStatusAndStartTimeBetween(
            venueId, BookingStatus.CANCELLED, thirtyDaysAgo, now));

    // Next 7 days of schedule, capped.
    Page<Booking> upcoming = bookingRepository
        .findByCourt_Venue_IdAndStartTimeBetweenOrderByStartTimeAsc(
            venueId, now, now.plusDays(7), PageRequest.of(0, UPCOMING_LIMIT));
    res.setUpcomingBookings(upcoming.getContent().stream().map(this::toEntry).toList());

    // Utilization over the trailing 30 days.
    res.setCourtUtilization(
        bookingRepository.courtUtilization(venueId, thirtyDaysAgo, now).stream()
            .map(p -> new VendorDashboardResponse.CourtUtilizationDto(
                p.getCourtId(),
                p.getCourtNumber(),
                p.getBookingCount() == null ? 0 : p.getBookingCount(),
                p.getBookedHours() == null ? 0 : p.getBookedHours()))
            .toList());

    return res;
  }

  // =====================================================================
  // Schedule (paginated, arbitrary window)
  // =====================================================================

  @Transactional(readOnly = true)
  public Page<VendorBookingEntry> getSchedule(UUID vendorId, UUID venueId,
                                              LocalDateTime from, LocalDateTime to,
                                              Pageable pageable) {
    requireOwnedVenue(vendorId, venueId);
    if (from == null || to == null || !to.isAfter(from)) {
      throw new BadRequestException("Invalid time window");
    }
    if (from.plusDays(92).isBefore(to)) {
      throw new BadRequestException("Window too large — max 92 days");
    }
    return bookingRepository
        .findByCourt_Venue_IdAndStartTimeBetweenOrderByStartTimeAsc(venueId, from, to, pageable)
        .map(this::toEntry);
  }

  // =====================================================================
  // No-show marking (the revenue-leakage tracker)
  // =====================================================================

  /**
   * Vendor marks a booking as a no-show. Rules:
   * <ul>
   *   <li>the booking's venue must belong to this vendor</li>
   *   <li>only CONFIRMED bookings can become NO_SHOW</li>
   *   <li>the slot must have already started (you can't no-show the future)</li>
   * </ul>
   */
  @Transactional
  public void markNoShow(UUID vendorId, UUID bookingId) {
    Booking booking = bookingRepository.findById(bookingId)
        .orElseThrow(() -> new NotFoundException("Booking not found"));

    UUID ownerId = booking.getCourt().getVenue().getOwner().getId();
    if (!ownerId.equals(vendorId)) {
      throw new ForbiddenException("This booking is not at one of your venues");
    }
    if (booking.getStatus() != BookingStatus.CONFIRMED) {
      throw new BadRequestException("Only confirmed bookings can be marked as no-show");
    }
    if (booking.getStartTime().isAfter(LocalDateTime.now())) {
      throw new BadRequestException("Cannot mark a future booking as no-show");
    }

    booking.setStatus(BookingStatus.NO_SHOW);
    bookingRepository.save(booking);
    // TODO(reliability): when the player reliability score lands, increment
    // the no-show counter on the player's profile here.
  }

  // --- helpers ---------------------------------------------------------

  private Venue requireOwnedVenue(UUID vendorId, UUID venueId) {
    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new NotFoundException("Venue not found"));
    if (venue.getOwner() == null || !venue.getOwner().getId().equals(vendorId)) {
      throw new ForbiddenException("You do not own this venue");
    }
    return venue;
  }

  private VendorBookingEntry toEntry(Booking b) {
    User player = b.getUser();
    String name = "Unknown";
    if (player != null) {
      String first = player.getFirstName() == null ? "" : player.getFirstName();
      String lastInitial = (player.getLastName() == null || player.getLastName().isEmpty())
          ? "" : (" " + player.getLastName().charAt(0) + ".");
      name = (first + lastInitial).trim();
    }
    return new VendorBookingEntry(
        b.getId(),
        b.getCourt() == null ? null : b.getCourt().getCourtNumber(),
        name,
        b.getStartTime(),
        b.getEndTime(),
        b.getStatus(),
        b.getPaymentStatus(),
        b.getTotalPrice());
  }
}
