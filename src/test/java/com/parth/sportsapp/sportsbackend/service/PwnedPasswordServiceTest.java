package com.parth.sportsapp.sportsbackend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the safe/guard paths of {@link PwnedPasswordService}.
 * The live HIBP k-anonymity call is exercised by integration/manual testing;
 * here we verify the service never blocks when disabled or given empty input,
 * and that "fail open" semantics hold when the network is unavailable.
 */
class PwnedPasswordServiceTest {

  @Test
  void returnsFalseWhenDisabled() {
    PwnedPasswordService svc = new PwnedPasswordService(false, "https://api.pwnedpasswords.com");
    assertThat(svc.isBreached("Password1!")).isFalse();
  }

  @Test
  void returnsFalseForNullOrEmpty() {
    PwnedPasswordService svc = new PwnedPasswordService(true, "https://api.pwnedpasswords.com");
    assertThat(svc.isBreached(null)).isFalse();
    assertThat(svc.isBreached("")).isFalse();
  }

  @Test
  void failsOpenWhenApiUnreachable() {
    // Point at an unroutable host so the HTTP call fails fast; the service must
    // NOT throw and must return false (allow), never blocking a real signup.
    PwnedPasswordService svc = new PwnedPasswordService(true, "https://127.0.0.1:1");
    assertThat(svc.isBreached("anything-here")).isFalse();
  }
}
