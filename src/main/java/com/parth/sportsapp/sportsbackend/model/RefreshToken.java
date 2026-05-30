package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A long-lived refresh token. We never store the token itself — only a SHA-256
 * hash of it — so a database leak does not hand an attacker usable tokens
 * (same principle as password hashing).
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true),
    @Index(name = "idx_refresh_user", columnList = "user_id")
})
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "expiry_date", nullable = false)
  private Instant expiryDate;

  @Column(name = "revoked", nullable = false)
  private boolean revoked = false;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public RefreshToken() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public Instant getExpiryDate() { return expiryDate; }
  public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }

  public boolean isRevoked() { return revoked; }
  public void setRevoked(boolean revoked) { this.revoked = revoked; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public boolean isExpired() {
    return Instant.now().isAfter(expiryDate);
  }
}
