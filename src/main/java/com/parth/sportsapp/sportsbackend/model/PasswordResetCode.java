package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single-use, short-lived password-reset code (6 digits, emailed to the user).
 * Only the SHA-256 hash is stored — a database leak reveals nothing usable.
 * Codes expire after 15 minutes and allow at most 5 wrong attempts.
 */
@Entity
@Table(name = "password_reset_codes")
public class PasswordResetCode {

  public static final int EXPIRY_MINUTES = 15;
  public static final int MAX_ATTEMPTS = 5;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "code_hash", nullable = false, length = 64)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false)
  private int attempts = 0;

  @Column(nullable = false)
  private boolean used = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  public PasswordResetCode() {}

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public String getCodeHash() { return codeHash; }
  public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

  public LocalDateTime getExpiresAt() { return expiresAt; }
  public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

  public int getAttempts() { return attempts; }
  public void setAttempts(int attempts) { this.attempts = attempts; }

  public boolean isUsed() { return used; }
  public void setUsed(boolean used) { this.used = used; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
