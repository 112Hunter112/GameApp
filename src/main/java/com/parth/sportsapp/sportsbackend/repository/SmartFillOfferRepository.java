package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.SmartFillOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SmartFillOfferRepository extends JpaRepository<SmartFillOffer, UUID> {

  /** Users already notified for this exact court + slot (dedup filter). */
  @Query("SELECT o.userId FROM SmartFillOffer o WHERE o.courtId = :courtId AND o.slotStart = :slotStart")
  List<UUID> findNotifiedUserIds(@Param("courtId") UUID courtId,
                                 @Param("slotStart") LocalDateTime slotStart);

  /** How many DISTINCT slots this court has offered since the cutoff (cooldown). */
  @Query("SELECT COUNT(DISTINCT o.slotStart) FROM SmartFillOffer o WHERE o.courtId = :courtId AND o.sentAt >= :since")
  long countDistinctSlotsSince(@Param("courtId") UUID courtId,
                               @Param("since") LocalDateTime since);
}
