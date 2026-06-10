package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Booking;
import com.parth.sportsapp.sportsbackend.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

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
}
