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
}
