package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  // clearAutomatically: bulk updates bypass the persistence context, so we clear
  // it afterward to prevent stale (un-revoked) entities being read back.
  @Modifying(clearAutomatically = true)
  @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
  int revokeAllForUser(@Param("userId") UUID userId);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :cutoff")
  int deleteExpired(@Param("cutoff") Instant cutoff);
}
