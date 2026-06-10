package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, UUID> {

  /** The newest still-usable code for a user (one active code at a time). */
  Optional<PasswordResetCode> findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(UUID userId);

  /** How many codes were requested recently — caps email-spam abuse. */
  long countByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after);

  /** Invalidate any previous codes when a new one is issued. */
  @Modifying
  @Query("UPDATE PasswordResetCode c SET c.used = true WHERE c.userId = :userId AND c.used = false")
  void invalidateAllForUser(@Param("userId") UUID userId);
}
