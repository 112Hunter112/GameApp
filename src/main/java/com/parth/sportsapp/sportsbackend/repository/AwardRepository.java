package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Award;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AwardRepository extends JpaRepository<Award, UUID> {

    /**
     * All awards for a user, newest first, with images eagerly loaded
     * to avoid N+1 queries on the list endpoint.
     */
    @Query("""
        SELECT DISTINCT a FROM Award a
        LEFT JOIN FETCH a.images
        LEFT JOIN FETCH a.sport
        WHERE a.user.id = :userId
        ORDER BY a.awardDate DESC, a.createdAt DESC
        """)
    List<Award> findByUserIdWithImages(@Param("userId") UUID userId);

    /** Count of awards for a user — useful for profile stats. */
    long countByUserId(UUID userId);
}
