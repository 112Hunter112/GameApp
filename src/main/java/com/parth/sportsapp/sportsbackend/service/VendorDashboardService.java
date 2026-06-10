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
  private final PlayerReliabilityService reliabilityService;
  private final NotificationService notificationService;

  public VendorDashboardService(BookingRepository bookingRepository,
                                VenueRepository venueRepository,
                                PlayerReliabilityService reliabilityService,
                                NotificationService notificationService) {
    this.bookingRepository = bookingRepository;
    this.venueRepository = venueRepository;
    this.reliabilityService = reliabilityService;
    this.notificationService = notificationService;
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

    // Next 7 days of schedule, capped, annotated with player reliability.
    Page<Booking> upcoming = bookingRepository
        .findByCourt_Venue_IdAndStartTimeBetweenOrderByStartTimeAsc(
            venueId, now, now.plusDays(7), PageRequest.of(0, UPCOMING_LIMIT));
    res.setUpcomingBookings(toAnnotatedEntries(upcoming.getContent()));

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
    Page<Booking> page = bookingRepository
        .findByCourt_Venue_IdAndStartTimeBetweenOrderByStartTimeAsc(venueId, from, to, pageable);
    List<VendorBookingEntry> annotated = toAnnotatedEntries(page.getContent());
    return new org.springframework.data.domain.PageImpl<>(annotated, pageable, page.getTotalElements());
  }

  // =====================================================================
  // Owner rollup — all venues combined (multi-venue owners)
  // =====================================================================

  @Transactional(readOnly = true)
  public com.parth.sportsapp.sportsbackend.dto.OwnerOverviewResponse getOwnerOverview(UUID vendorId) {
    List<Venue> venues = venueRepository.findByOwner_Id(vendorId);

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
    LocalDateTime startOfWeek  = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
    LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
    LocalDateTime thirtyDaysAgo = now.minusDays(30);

    var overview = new com.parth.sportsapp.sportsbackend.dto.OwnerOverviewResponse();
    java.math.BigDecimal weekTotal = java.math.BigDecimal.ZERO;
    java.math.BigDecimal monthTotal = java.math.BigDecimal.ZERO;
    long today = 0, noShows = 0, cancels = 0;
    var perVenue = new java.util.ArrayList<com.parth.sportsapp.sportsbackend.dto.OwnerOverviewResponse.VenueSummary>();

    for (Venue v : venues) {
      java.math.BigDecimal vWeek = bookingRepository.sumRevenue(
          v.getId(), REVENUE_STATUSES, startOfWeek, now);
      java.math.BigDecimal vMonth = bookingRepository.sumRevenue(
          v.getId(), REVENUE_STATUSES, startOfMonth, now);
      long vToday = bookingRepository.countByCourt_Venue_IdAndStatusAndStartTimeBetween(
          v.getId(), BookingStatus.CONFIRMED, startOfToday, startOfToday.plusDays(1));
      long vNoShows = bookingRepository.countByCourt_Venue_IdAndStatusAndStartTimeBetween(
          v.getId(), BookingStatus.NO_SHOW, thirtyDaysAgo, now);
      long vCancels = bookingRepository.countByCourt_Venue_IdAndStatusAndStartTimeBetween(
          v.getId(), BookingStatus.CANCELLED, thirtyDaysAgo, now);

      weekTotal = weekTotal.add(vWeek);
      monthTotal = monthTotal.add(vMonth);
      today += vToday;
      noShows += vNoShows;
      cancels += vCancels;

      var s = new com.parth.sportsapp.sportsbackend.dto.OwnerOverviewResponse.VenueSummary();
      s.setVenueId(v.getId());
      s.setVenueName(v.getName());
      s.setRevenueThisMonth(vMonth);
      s.setBookingsToday(vToday);
      perVenue.add(s);
    }

    // Biggest earners first.
    perVenue.sort((a, b) -> b.getRevenueThisMonth().compareTo(a.getRevenueThisMonth()));

    overview.setTotalVenues(venues.size());
    overview.setRevenueThisWeek(weekTotal);
    overview.setRevenueThisMonth(monthTotal);
    overview.setBookingsToday(today);
    overview.setNoShowsLast30Days(noShows);
    overview.setCancellationsLast30Days(cancels);
    overview.setVenues(perVenue);
    return overview;
  }

  // =====================================================================
  // Hour-of-week occupancy heatmap
  // =====================================================================

  @Transactional(readOnly = true)
  public com.parth.sportsapp.sportsbackend.dto.HeatmapResponse getHeatmap(
      UUID vendorId, UUID venueId, int windowDays) {
    requireOwnedVenue(vendorId, venueId);
    if (windowDays < 7 || windowDays > 365) {
      throw new BadRequestException("windowDays must be 7-365");
    }
    LocalDateTime now = LocalDateTime.now();
    var cells = bookingRepository.occupancyHeatmap(venueId, now.minusDays(windowDays), now)
        .stream()
        .map(c -> new com.parth.sportsapp.sportsbackend.dto.HeatmapResponse.Cell(
            c.getDayOfWeek() == null ? 0 : c.getDayOfWeek(),
            c.getHourOfDay() == null ? 0 : c.getHourOfDay(),
            c.getBookingCount() == null ? 0 : c.getBookingCount()))
        .toList();
    return new com.parth.sportsapp.sportsbackend.dto.HeatmapResponse(venueId, windowDays, cells);
  }

  // =====================================================================
  // CSV export — for the vendor's accountant
  // =====================================================================

  /** Same data as the schedule view, as a downloadable CSV. */
  @Transactional(readOnly = true)
  public String exportBookingsCsv(UUID vendorId, UUID venueId,
                                  LocalDateTime from, LocalDateTime to) {
    requireOwnedVenue(vendorId, venueId);
    if (from == null || to == null || !to.isAfter(from)) {
      throw new BadRequestException("Invalid time window");
    }
    if (from.plusDays(366).isBefore(to)) {
      throw new BadRequestException("Window too large — max 1 year");
    }

    Page<Booking> page = bookingRepository
        .findByCourt_Venue_IdAndStartTimeBetweenOrderByStartTimeAsc(
            venueId, from, to, PageRequest.of(0, 10_000));

    StringBuilder csv = new StringBuilder(
        "booking_id,court,player,start_time,end_time,status,payment_status,total_price\n");
    for (Booking b : page.getContent()) {
      VendorBookingEntry e = toEntry(b);
      csv.append(csvField(e.getBookingId()))
          .append(',').append(csvField(e.getCourtNumber()))
          .append(',').append(csvField(e.getPlayerName()))
          .append(',').append(csvField(e.getStartTime()))
          .append(',').append(csvField(e.getEndTime()))
          .append(',').append(csvField(e.getStatus()))
          .append(',').append(csvField(e.getPaymentStatus()))
          .append(',').append(csvField(e.getTotalPrice()))
          .append('\n');
    }
    return csv.toString();
  }

  /** Quote-and-escape a CSV field (handles commas, quotes, nulls). */
  private static String csvField(Object value) {
    if (value == null) return "";
    String s = value.toString();
    if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
      return '"' + s.replace("\"", "\"\"") + '"';
    }
    return s;
  }

  // =====================================================================
  // Revenue left on the table
  // =====================================================================

  /**
   * Estimates what each court's EMPTY hours would have earned over the
   * trailing window. The open-hours assumption is a flat hours/day figure
   * for now — swap in real venue opening hours once that JSONB schema is
   * finalized. This is the report that quantifies under-utilization for
   * the vendor ("Court 2 left ~$440 unbooked last month").
   */
  @Transactional(readOnly = true)
  public com.parth.sportsapp.sportsbackend.dto.RevenueReportResponse getRevenueReport(
      UUID vendorId, UUID venueId, int windowDays, int openHoursPerDay) {
    Venue venue = requireOwnedVenue(vendorId, venueId);
    if (windowDays < 1 || windowDays > 365) {
      throw new BadRequestException("windowDays must be 1-365");
    }
    if (openHoursPerDay < 1 || openHoursPerDay > 24) {
      throw new BadRequestException("openHoursPerDay must be 1-24");
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime from = now.minusDays(windowDays);
    double bookableHours = (double) windowDays * openHoursPerDay;

    var report = new com.parth.sportsapp.sportsbackend.dto.RevenueReportResponse();
    report.setVenueId(venue.getId());
    report.setVenueName(venue.getName());
    report.setWindowDays(windowDays);
    report.setAssumedOpenHoursPerDay(openHoursPerDay);

    java.math.BigDecimal totalMissed = java.math.BigDecimal.ZERO;
    double totalEmpty = 0;
    var rows = new java.util.ArrayList<com.parth.sportsapp.sportsbackend.dto.RevenueReportResponse.CourtRevenueRow>();

    for (var u : bookingRepository.courtUtilization(venueId, from, now)) {
      double booked = u.getBookedHours() == null ? 0 : u.getBookedHours();
      double empty = Math.max(0, bookableHours - booked);
      java.math.BigDecimal rate = u.getHourlyRate() == null
          ? java.math.BigDecimal.ZERO : u.getHourlyRate();
      java.math.BigDecimal missed = rate.multiply(java.math.BigDecimal.valueOf(empty))
          .setScale(2, java.math.RoundingMode.HALF_UP);

      var row = new com.parth.sportsapp.sportsbackend.dto.RevenueReportResponse.CourtRevenueRow();
      row.setCourtId(u.getCourtId());
      row.setCourtNumber(u.getCourtNumber());
      row.setHourlyRate(rate);
      row.setBookedHours(Math.round(booked * 10.0) / 10.0);
      row.setEmptyHours(Math.round(empty * 10.0) / 10.0);
      row.setUtilizationPct(bookableHours == 0 ? 0
          : Math.round(booked / bookableHours * 1000.0) / 10.0);
      row.setMissedRevenue(missed);
      rows.add(row);

      totalMissed = totalMissed.add(missed);
      totalEmpty += empty;
    }

    // Worst offenders first — that's what the vendor needs to act on.
    rows.sort((a, b) -> b.getMissedRevenue().compareTo(a.getMissedRevenue()));
    report.setCourts(rows);
    report.setTotalMissedRevenue(totalMissed);
    report.setTotalEmptyHours(Math.round(totalEmpty * 10.0) / 10.0);
    return report;
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
    booking.setNoShowMarkedAt(LocalDateTime.now());
    bookingRepository.save(booking);

    // Fairness: the player must know — their reliability score is affected.
    // (Reliability itself is computed live from booking statuses, so no
    // counters to increment here.)
    if (booking.getUser() != null) {
      notificationService.sendNoShowMarked(
          booking.getUser(),
          booking.getId(),
          booking.getCourt().getVenue().getName());
    }
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

  /**
   * Map bookings to entries and annotate each with the player's network-wide
   * reliability. ONE batch query for the whole page (no N+1).
   */
  private List<VendorBookingEntry> toAnnotatedEntries(List<Booking> bookings) {
    var userIds = bookings.stream()
        .map(b -> b.getUser() == null ? null : b.getUser().getId())
        .filter(java.util.Objects::nonNull)
        .collect(java.util.stream.Collectors.toSet());
    var reliability = reliabilityService.forUsers(userIds);

    return bookings.stream().map(b -> {
      VendorBookingEntry e = toEntry(b);
      if (b.getUser() != null) {
        var r = reliability.get(b.getUser().getId());
        if (r != null) {
          e.setPlayerReliabilityTier(r.tier().name());
          e.setPlayerNoShowRate(Math.round(r.noShowRate() * 100.0) / 100.0);
        }
      }
      return e;
    }).toList();
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
