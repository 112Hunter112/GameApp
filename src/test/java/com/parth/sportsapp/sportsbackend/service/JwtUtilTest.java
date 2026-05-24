package com.parth.sportsapp.sportsbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for {@link JwtUtil}. No Spring context — we wire the @Value
 * fields manually via reflection and invoke the @PostConstruct ourselves.
 * These tests pin down the security-hardening behavior:
 *   - boot fails fast on bad secret config
 *   - malformed / tampered tokens return false from validateToken (no 500)
 *   - happy-path round-trip works
 */
class JwtUtilTest {

  private static final String VALID_SECRET = Base64.getEncoder().encodeToString(new byte[32]); // 256 bits
  private static final long EXPIRATION = 60_000L; // 1 minute

  private JwtUtil util;

  @BeforeEach
  void setUp() {
    util = newUtilWith(VALID_SECRET, EXPIRATION);
  }

  // --- boot-time validation -------------------------------------------------

  @Test
  void initRejectsBlankSecret() {
    JwtUtil bad = new JwtUtil();
    ReflectionTestUtils.setField(bad, "SECRET_KEY", "");
    ReflectionTestUtils.setField(bad, "EXPIRATION_TIME", EXPIRATION);

    assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(bad, "init"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("jwt.secret");
  }

  @Test
  void initRejectsSecretShorterThan256Bits() {
    // 16 bytes = 128 bits = too short for HS256
    String tooShort = Base64.getEncoder().encodeToString(new byte[16]);

    JwtUtil bad = new JwtUtil();
    ReflectionTestUtils.setField(bad, "SECRET_KEY", tooShort);
    ReflectionTestUtils.setField(bad, "EXPIRATION_TIME", EXPIRATION);

    assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(bad, "init"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("256 bits");
  }

  @Test
  void initRejectsNonPositiveExpiration() {
    JwtUtil bad = new JwtUtil();
    ReflectionTestUtils.setField(bad, "SECRET_KEY", VALID_SECRET);
    ReflectionTestUtils.setField(bad, "EXPIRATION_TIME", 0L);

    assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(bad, "init"))
        .isInstanceOf(IllegalStateException.class);
  }

  // --- happy path -----------------------------------------------------------

  @Test
  void generateAndExtractRoundTrip() {
    UUID userId = UUID.randomUUID();
    String token = util.generateToken("user@example.com", userId, "USER");

    assertThat(util.extractEmail(token)).isEqualTo("user@example.com");
    assertThat(util.extractUserId(token)).isEqualTo(userId.toString());
    assertThat(util.extractRole(token)).isEqualTo("ROLE_USER");
    assertThat(util.validateToken(token, "user@example.com")).isTrue();
  }

  @Test
  void validateRejectsWrongEmail() {
    String token = util.generateToken("user@example.com", UUID.randomUUID(), "USER");

    assertThat(util.validateToken(token, "someone-else@example.com")).isFalse();
  }

  // --- the regression we hardened against ----------------------------------

  @Test
  void validateReturnsFalseForGarbageTokenInsteadOfThrowing() {
    // Previously this would propagate a JwtException up the filter chain.
    // After hardening, validateToken swallows it and returns false.
    assertThat(util.validateToken("not.a.real.jwt", "user@example.com")).isFalse();
    assertThat(util.validateToken("", "user@example.com")).isFalse();
  }

  @Test
  void validateRejectsTamperedSignature() {
    String token = util.generateToken("user@example.com", UUID.randomUUID(), "USER");
    // Replace the entire signature segment with something that can't possibly verify.
    int lastDot = token.lastIndexOf('.');
    String tampered = token.substring(0, lastDot + 1) + "tampered_signature";

    assertThat(util.validateToken(tampered, "user@example.com")).isFalse();
  }

  @Test
  void validateRejectsExpiredToken() throws InterruptedException {
    JwtUtil quick = newUtilWith(VALID_SECRET, 1L); // expires in 1 ms
    String token = quick.generateToken("user@example.com", UUID.randomUUID(), "USER");

    Thread.sleep(20);

    assertThat(quick.validateToken(token, "user@example.com")).isFalse();
  }

  // --- helpers --------------------------------------------------------------

  private static JwtUtil newUtilWith(String secret, long expiration) {
    JwtUtil u = new JwtUtil();
    ReflectionTestUtils.setField(u, "SECRET_KEY", secret);
    ReflectionTestUtils.setField(u, "EXPIRATION_TIME", expiration);
    ReflectionTestUtils.invokeMethod(u, "init");
    return u;
  }
}
