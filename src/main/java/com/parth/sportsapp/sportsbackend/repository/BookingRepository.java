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
 * Booking data access: booking-flow queries (overlap detection, availability,
 * player/owner views) plus the vendor-dashboard aggregates (revenue,
 * utilization, heatmap, reliability counts).
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

  // =====================================================================
  // Booking flow — conflict detection & availability
  // =====================================================================

  /**
   * Overlap check: two intervals [s1,e1) and [s2,e2) collide iff s1 < e2 AND e1 > s2.
   * Only PENDING/CONFIRMED bookings hold the slot. Must be called while holding the
   * pessimistic lock on the court row (see CourtRepository#findByIdForUpdate) so two
   * simultaneous requests for the same court serialize instead of double-booking.
   */
  @Query("""
      SELECT COUNT(b) FROM Booking b
      WHERE b.court.id = :courtId
        AND b.status IN (com.parth.sportsapp.sportsbackend.model.BookingStatus.PENDING,
                         com.parth.sportsapp.sportsbackend.model.BookingStatus.CONFIRMED)
        AND b.startTime < :endTime
        AND b.endTime > :startTime
      """)
  long countConflicts(@Param("courtId") UUID courtId,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

  /** All slot-holding bookings for a court within a window — used to paint the availability grid. */
  @Query("""
      SELECT b FROM Booking b
      WHERE b.court.id = :courtId
        AND b.status IN (com.parth.sportsapp.sportsbackend.model.BookingStatus.PENDING,
                         com.parth.sportsapp.sportsbackend.model.BookingStatus.CONFIRMED)
        AND b.startTime < :windowEnd
        AND b.endTime > :windowStart
      ORDER BY b.startTime
      """)
  List<Booking> findActiveInWindow(@Param("courtId") UUID courtId,
                                   @Param("windowStart") LocalDateTime windowStart,
                                   @Param("windowEnd") LocalDateTime windowEnd);

  // --- Player views -----------------------------------------------------------

  Page<Booking> findByUser_IdAndEndTimeGreaterThanEqualAndStatusInOrderByStartTimeAsc(
      UUID userId, LocalDateTime now, List<BookingStatus> statuses, Pageable pageable);

  @Query("""
      SELECT b FROM Booking b
      WHERE b.user.id = :userId
        AND (b.endTime < :now
             OR b.status IN (com.parth.sportsapp.sportsbackend.model.BookingStatus.CANCELLED,
                             com.parth.sportsapp.sportsbackend.model.BookingStatus.DECLINED,
                             com.parth.sportsapp.sportsbackend.model.BookingStatus.COMPLETED))
      ORDER BY b.startTime DESC
      """)
  Page<Booking> findPastForUser(@Param("userId") UUID userId,
                                @Param("now") LocalDateTime now,
                                Pageable pageable);

  /** A user's own bookings, newest first. */
  Page<Booking> findByUser_IdOrderByStartTimeDesc(UUID userId, Pageable pageable);

  // --- Owner views ------------------------------------------------------------

  /** Dashboard query: every booking across all venues this owner runs, with optional filters. */
  @Query("""
      SELECT b FROM Booking b
      WHERE b.court.venue.owner.id = :ownerId
        AND (:venueId IS NULL OR b.court.venue.id = :venueId)
        AND (:status IS NULL OR b.status = :status)
        AND (CAST(:dayStart AS timestamp) IS NULL OR b.startTime >= :dayStart)
        AND (CAST(:dayEnd AS timestamp) IS NULL OR b.startTime < :dayEnd)
      ORDER BY b.startTime ASC
      """)
  Page<Booking> findForOwner(@Param("ownerId") UUID ownerId,
                             @Param("venueId") UUID venueId,
                             @Param("status") BookingStatus status,
                             @Param("dayStart") LocalDateTime dayStart,
                             @Param("dayEnd") LocalDateTime dayEnd,
                             Pageable pageable);

  /** Badge count for the owner's "requests waiting" indicator. */
  @Query("""
      SELECT COUNT(b) FROM Booking b
      WHERE b.court.venue.owner.id = :ownerId
        AND b.status = com.parth.sportsapp.sportsbackend.model.BookingStatus.PENDING
        AND b.startTime > :now
      """)
  long countPendingForOwner(@Param("ownerId") UUID ownerId, @Param("now") LocalDateTime now);

  // =====================================================================
  // Vendor dashboard — read/aggregate queries
  // =====================================================================

  /** Schedule view: all bookings across a venue's courts in a time window. */
  Page<Booking> findByCourt_Venue_IdAndStartTimeBetweenOrderByStartTimeAsc(
      UUID venueId, LocalDateTime from, LocalDateTime to, Pageable pageable);

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
}
