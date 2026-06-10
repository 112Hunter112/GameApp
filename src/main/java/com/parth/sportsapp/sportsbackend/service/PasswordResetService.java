package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.model.PasswordResetCode;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.PasswordResetCodeRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Forgot-password flow, mobile-first (emailed 6-digit code, not a click link).
 *
 * Security properties:
 *  - No user enumeration: requesting a code for an unknown email returns the
 *    same success response as a known one.
 *  - Codes are stored as SHA-256 hashes, expire after 15 minutes, are
 *    single-use, and lock after 5 wrong attempts.
 *  - Max 3 code requests per hour per account (email-bombing guard).
 *  - A successful reset revokes EVERY refresh token (logs out all devices).
 */
@Service
@Transactional
public class PasswordResetService {

  private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int MAX_REQUESTS_PER_HOUR = 3;

  @Autowired private UserRepository userRepository;
  @Autowired private PasswordResetCodeRepository codeRepository;
  @Autowired private EmailService emailService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private PwnedPasswordService pwnedPasswordService;
  @Autowired private RefreshTokenService refreshTokenService;

  /** Step 1: user asks for a code. Always "succeeds" from the caller's view. */
  public void requestCode(String email) {
    Optional<User> userOpt = userRepository.findByEmail(normalize(email));
    if (userOpt.isEmpty()) {
      // Same outward behaviour as success — do not reveal that the email is unknown.
      log.info("Password reset requested for unknown email");
      return;
    }
    User user = userOpt.get();

    // Google-only accounts have a random unusable password; reset still works
    // and effectively ADDS email/password login for them, which is fine.

    if (codeRepository.countByUserIdAndCreatedAtAfter(
        user.getId(), LocalDateTime.now().minusHours(1)) >= MAX_REQUESTS_PER_HOUR) {
      log.warn("Password reset rate limit hit for user {}", user.getId());
      return; // silently drop — still no signal to the caller
    }

    // One active code at a time
    codeRepository.invalidateAllForUser(user.getId());

    String code = String.format("%06d", RANDOM.nextInt(1_000_000));
    PasswordResetCode entity = new PasswordResetCode();
    entity.setUserId(user.getId());
    entity.setCodeHash(sha256Hex(code));
    entity.setExpiresAt(LocalDateTime.now().plusMinutes(PasswordResetCode.EXPIRY_MINUTES));
    codeRepository.save(entity);

    emailService.sendPasswordResetCode(user.getEmail(), user.getFirstName(), code);
  }

  /** Step 2: user submits code + new password. */
  public void resetPassword(String email, String code, String newPassword) {
    // Deliberately vague error for every failure mode — prevents probing.
    final BadRequestException invalid =
        new BadRequestException("Invalid or expired code. Request a new one and try again.");

    User user = userRepository.findByEmail(normalize(email)).orElseThrow(() -> invalid);

    PasswordResetCode stored = codeRepository
        .findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
        .orElseThrow(() -> invalid);

    if (stored.isExpired() || stored.getAttempts() >= PasswordResetCode.MAX_ATTEMPTS) {
      throw invalid;
    }

    if (!stored.getCodeHash().equals(sha256Hex(code == null ? "" : code.trim()))) {
      stored.setAttempts(stored.getAttempts() + 1);
      codeRepository.save(stored);
      throw invalid;
    }

    // Code is right — now validate the new password (clear errors are fine here).
    validatePassword(newPassword);

    stored.setUsed(true);
    codeRepository.save(stored);

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    // Kick every session: if the reset was triggered by a hijack, this ends it.
    refreshTokenService.revokeAllForUser(user.getId());
    log.info("Password reset completed for user {}", user.getId());
  }

  private void validatePassword(String password) {
    if (password == null || password.length() < 8) {
      throw new BadRequestException("Password must be at least 8 characters");
    }
    if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
      throw new BadRequestException("Password must contain at least one letter and one number");
    }
    if (pwnedPasswordService.isBreached(password)) {
      throw new BadRequestException(
          "This password has appeared in a data breach. Please choose a different one.");
    }
  }

  private String normalize(String email) {
    return email == null ? "" : email.trim().toLowerCase();
  }

  private String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
