package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.UserPreference;
import com.parth.sportsapp.sportsbackend.model.UserPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UserPreferenceId> {

  /** All of one user's preferences, one row per sport. */
  List<UserPreference> findByUserId(UUID userId);

  /** A specific user's preference for a specific sport. */
  Optional<UserPreference> findByUser_IdAndSports_Id(UUID userId, UUID sportId);

  boolean existsByUser_IdAndSports_Id(UUID userId, UUID sportId);

  /** Which sport (if any) the user has marked as primary. */
  Optional<UserPreference> findByUser_IdAndIsPrimarySportTrue(UUID userId);

  long countByUser_Id(UUID userId);

  /**
   * Bulk-unset the primary flag for a user. Used inside setPrimary so we can
   * atomically demote the previous primary before promoting a new one.
   * clearAutomatically empties the persistence context so a follow-up read of
   * the (now non-primary) row returns fresh state instead of a stale cache hit.
   */
  @Modifying(clearAutomatically = true)
  @Query("UPDATE UserPreference p SET p.isPrimarySport = false WHERE p.user.id = :userId AND p.isPrimarySport = true")
  int clearPrimaryForUser(@Param("userId") UUID userId);

  /**
   * Smart Fill targeting: players who
   * <ul>
   *   <li>play the given sport and are open to matchmaking,</li>
   *   <li>are available on the slot's day type (weekday/weekend),</li>
   *   <li>were last seen within their own notification radius of the venue
   *       (default 15 km when they haven't set one).</li>
   * </ul>
   * Ordered by distance so the closest players are notified first when the
   * candidate pool exceeds the cap.
   */
  @Query(value = """
      SELECT u.id         AS user_id,
             u.first_name AS first_name
      FROM user_preferences p
      JOIN users u ON u.id = p.user_id
      WHERE p.sport_id = :sportId
        AND p.open_to_matchmaking = true
        AND (CASE WHEN :isWeekend THEN p.available_weekends
                  ELSE p.available_weekdays END) = true
        AND u.last_known_location IS NOT NULL
        AND ST_DWithin(
            u.last_known_location::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
            COALESCE(p.notification_radius_meters, 15000)
        )
      ORDER BY ST_Distance(
            u.last_known_location::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
        ) ASC
      LIMIT :maxCandidates
      """, nativeQuery = true)
  List<SmartFillCandidateProjection> findSmartFillCandidates(
      @Param("sportId") UUID sportId,
      @Param("lat") double venueLat,
      @Param("lng") double venueLng,
      @Param("isWeekend") boolean isWeekend,
      @Param("maxCandidates") int maxCandidates);
}
