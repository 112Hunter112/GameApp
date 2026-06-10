package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Booking;
import com.parth.sportsapp.sportsbackend.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Booking data access.
 *
 * <p><b>Currently: vendor-dashboard READ queries only.</b></p>
 *
 * <p>The booking-FLOW queries (overlap detection with {@code tstzrange &&},
 * availability slots, etc.) are intentionally not here yet — they're being
 * built as part of the booking-service work. Add them below when ready.</p>
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

  // =====================================================================
  // Vendor dashboard — read/aggregate queries
  // =====================================================================

  /** Schedule view: all bookings across a venue's courts in a time window. */
  Page<Booking> findByCourt_Venue_IdAndStartTimeBetweenOrderByStartTimeAsc(
      UUID venueId, LocalDateTime from, LocalDateTime to, Pageable pageable);

  /** A user's own bookings, newest first (for the player side later). */
  Page<Booking> findByUser_IdOrderByStartTimeDesc(UUID userId, Pageable pageable);

  long countByCourt_Venue_IdAndStatusAndStartTimeBetween(
      UUID venueId, BookingStatus status, LocalDateTime from, LocalDateTime to);

  /**
   * Revenue for a venue in a window. Counts CONFIRMED + COMPLETED bookings.
   * Note: refunds are not netted out yet — revisit when payment flow lands.
   */
  @Query("""
      SELECT COALESCE(SUM(b.totalPrice), 0)
      FROM Booking b
      WHERE b.court.venue.id = :venueId
        AND b.status IN :statuses
        AND b.startTime >= :from AND b.startTime < :to
      """)
  BigDecimal sumRevenue(@Param("venueId") UUID venueId,
                        @Param("statuses") Collection<BookingStatus> statuses,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

  /**
   * Per-court utilization: booking count + total booked hours in a window.
   * LEFT JOIN so courts with zero bookings still appear (those are the ones
   * the vendor needs to see — empty courts are lost revenue).
   */
  @Query(value = """
      SELECT c.id                                    AS court_id,
             c.court_number                          AS court_number,
             c.hourly_rate                           AS hourly_rate,
             COUNT(b.id)                             AS booking_count,
             COALESCE(SUM(EXTRACT(EPOCH FROM (b.end_time - b.start_time)) / 3600.0), 0) AS booked_hours
      FROM courts c
      LEFT JOIN bookings b
             ON b.court_id = c.id
            AND b.status IN ('CONFIRMED', 'COMPLETED')
            AND b.start_time >= :from AND b.start_time < :to
      WHERE c.venue_id = :venueId
      GROUP BY c.id, c.court_number, c.hourly_rate
      ORDER BY booked_hours DESC
      """, nativeQuery = true)
  List<CourtUtilizationProjection> courtUtilization(@Param("venueId") UUID venueId,
                                                    @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

  /**
   * Hour-of-week occupancy heatmap: booking counts grouped by ISO day-of-week
   * (1 = Monday … 7 = Sunday) and hour of day. The grid that shows a vendor
   * exactly WHERE their dead hours are — and therefore where to aim Smart Fill.
   */
  @Query(value = """
      SELECT EXTRACT(ISODOW FROM b.start_time)::int AS day_of_week,
             EXTRACT(HOUR   FROM b.start_time)::int AS hour_of_day,
             COUNT(*)                                AS booking_count
      FROM bookings b
      JOIN courts c ON c.id = b.court_id
      WHERE c.venue_id = :venueId
        AND b.status IN ('CONFIRMED', 'COMPLETED')
        AND b.start_time >= :from AND b.start_time < :to
      GROUP BY 1, 2
      ORDER BY 1, 2
      """, nativeQuery = true)
  List<HeatmapCellProjection> occupancyHeatmap(@Param("venueId") UUID venueId,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

  // =====================================================================
  // Player reliability — counts per status for a set of users in one query
  // (avoids N+1 when annotating a schedule page with reliability tiers).
  // =====================================================================

  @Query("""
      SELECT b.user.id AS userId, b.status AS status, COUNT(b) AS cnt
      FROM Booking b
      WHERE b.user.id IN :userIds
      GROUP BY b.user.id, b.status
      """)
  List<UserStatusCountProjection> countStatusesForUsers(@Param("userIds") Collection<UUID> userIds);

  // =====================================================================
  // TODO(booking-flow): add overlap detection here when building the
  // booking service. Pattern:
  //   tstzrange(start_time, end_time, '[)') && tstzrange(:newStart, :newEnd, '[)')
  // =====================================================================
}
