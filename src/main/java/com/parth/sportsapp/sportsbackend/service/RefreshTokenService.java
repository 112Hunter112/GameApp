package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.AuthResponse;
import com.parth.sportsapp.sportsbackend.exception.UnauthorizedException;
import com.parth.sportsapp.sportsbackend.model.RefreshToken;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserRole;
import com.parth.sportsapp.sportsbackend.repository.RefreshTokenRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Issues, rotates, and revokes refresh tokens.
 *
 * <p>Design:
 * <ul>
 *   <li>The plaintext token is a 256-bit random value, returned to the client once.</li>
 *   <li>Only its SHA-256 hash is stored, so a DB leak yields no usable tokens.</li>
 *   <li>Each refresh ROTATES the token: the old one is revoked, a new one issued.</li>
 *   <li>Reuse of an already-revoked token is treated as theft — ALL of that user's
 *       tokens are revoked, forcing re-login everywhere.</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;
  private final long refreshExpirationMs;

  public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                             UserRepository userRepository,
                             JwtUtil jwtUtil,
                             @Value("${jwt.refresh-expiration}") long refreshExpirationMs) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.userRepository = userRepository;
    this.jwtUtil = jwtUtil;
    this.refreshExpirationMs = refreshExpirationMs;
  }

  /** Create and persist a new refresh token for the user; returns the plaintext token. */
  @Transactional
  public String issue(UUID userId) {
    String plaintext = generateOpaqueToken();
    RefreshToken entity = new RefreshToken();
    entity.setTokenHash(sha256Hex(plaintext));
    entity.setUserId(userId);
    entity.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
    entity.setRevoked(false);
    entity.setCreatedAt(Instant.now());
    refreshTokenRepository.save(entity);
    return plaintext;
  }

  /**
   * Exchange a valid refresh token for a new access token + new refresh token.
   * Rotates the refresh token and detects reuse of revoked tokens.
   */
  @Transactional
  public AuthResponse refresh(String plaintextToken) {
    if (plaintextToken == null || plaintextToken.isBlank()) {
      throw new UnauthorizedException("Refresh token is required");
    }

    RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256Hex(plaintextToken))
        .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    // Reuse detection: a revoked token being presented means it was likely stolen
    // (the legitimate client already rotated it). Nuke all sessions for safety.
    if (stored.isRevoked()) {
      refreshTokenRepository.revokeAllForUser(stored.getUserId());
      log.warn("Refresh token reuse detected for user {}; revoked all sessions", stored.getUserId());
      throw new UnauthorizedException("Invalid refresh token");
    }

    if (stored.isExpired()) {
      throw new UnauthorizedException("Refresh token expired");
    }

    User user = userRepository.findById(stored.getUserId())
        .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    // Rotate: revoke the presented token, issue a fresh one.
    stored.setRevoked(true);
    refreshTokenRepository.save(stored);

    String role = user.getRole() == null ? UserRole.USER.name() : user.getRole().name();
    String accessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), role);
    String newRefresh = issue(user.getId());

    return new AuthResponse(
        accessToken,
        newRefresh,
        jwtUtil.getAccessTokenExpiryMs() / 1000,
        user.getEmail(),
        user.getFirstName(),
        role
    );
  }

  /** Revoke a single refresh token (logout on this device). Idempotent. */
  @Transactional
  public void revoke(String plaintextToken) {
    if (plaintextToken == null || plaintextToken.isBlank()) return;
    refreshTokenRepository.findByTokenHash(sha256Hex(plaintextToken)).ifPresent(t -> {
      t.setRevoked(true);
      refreshTokenRepository.save(t);
    });
  }

  /** Revoke every refresh token for a user (logout everywhere / password change). */
  @Transactional
  public void revokeAllForUser(UUID userId) {
    refreshTokenRepository.revokeAllForUser(userId);
  }

  /**
   * Nightly cleanup of refresh tokens past their expiry. Without this the
   * table grows forever as legitimate tokens age out. 3 AM is chosen for low
   * traffic; cron string is "second minute hour day-of-month month day-of-week".
   */
  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void purgeExpiredTokens() {
    int removed = refreshTokenRepository.deleteExpired(Instant.now());
    if (removed > 0) {
      log.info("Refresh token cleanup: removed {} expired tokens", removed);
    }
  }

  // --- helpers --------------------------------------------------------------

  private static String generateOpaqueToken() {
    byte[] bytes = new byte[32]; // 256 bits
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16));
        sb.append(Character.forDigit((b & 0xF), 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
