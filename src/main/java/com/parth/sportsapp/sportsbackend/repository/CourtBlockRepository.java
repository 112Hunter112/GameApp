package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.CourtBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CourtBlockRepository extends JpaRepository<CourtBlock, UUID> {

  /** Overlap test, same semantics as booking conflicts: s1 < e2 AND e1 > s2. */
  @Query("""
      SELECT COUNT(b) FROM CourtBlock b
      WHERE b.court.id = :courtId
        AND b.startTime < :endTime
        AND b.endTime > :startTime
      """)
  long countOverlapping(@Param("courtId") UUID courtId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

  @Query("""
      SELECT b FROM CourtBlock b
      WHERE b.court.id = :courtId
        AND b.startTime < :windowEnd
        AND b.endTime > :windowStart
      ORDER BY b.startTime
      """)
  List<CourtBlock> findInWindow(@Param("courtId") UUID courtId,
                                @Param("windowStart") LocalDateTime windowStart,
                                @Param("windowEnd") LocalDateTime windowEnd);

  /** All blocks across an owner's venues for one day — feeds the host calendar. */
  @Query("""
      SELECT b FROM CourtBlock b
      WHERE b.court.venue.owner.id = :ownerId
        AND (:venueId IS NULL OR b.court.venue.id = :venueId)
        AND b.startTime < :dayEnd
        AND b.endTime > :dayStart
      ORDER BY b.startTime
      """)
  List<CourtBlock> findForOwner(@Param("ownerId") UUID ownerId,
                                @Param("venueId") UUID venueId,
                                @Param("dayStart") LocalDateTime dayStart,
                                @Param("dayEnd") LocalDateTime dayEnd);
}
