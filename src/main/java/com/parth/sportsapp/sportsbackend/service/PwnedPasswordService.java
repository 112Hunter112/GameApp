package com.parth.sportsapp.sportsbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Checks a candidate password against the Have I Been Pwned "Pwned Passwords"
 * corpus using k-anonymity, per NIST SP 800-63B guidance (screen new passwords
 * against known-breached lists).
 *
 * <p>Privacy: we SHA-1 the password, send only the first 5 hex characters of
 * the hash to the API, and compare the returned suffixes locally. The full
 * password and full hash never leave this server.</p>
 *
 * <p>Availability: if the API is unreachable we "fail open" (allow the password)
 * so a third-party outage cannot block all sign-ups. The event is logged.</p>
 */
@Service
public class PwnedPasswordService {

  private static final Logger log = LoggerFactory.getLogger(PwnedPasswordService.class);

  private final RestClient restClient;
  private final boolean enabled;

  public PwnedPasswordService(
      @Value("${app.security.pwned-check.enabled:true}") boolean enabled,
      @Value("${app.security.pwned-check.base-url:https://api.pwnedpasswords.com}") String baseUrl) {
    this.enabled = enabled;
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  /**
   * @return true if the password appears in a known breach corpus and should be rejected.
   */
  public boolean isBreached(String rawPassword) {
    if (!enabled || rawPassword == null || rawPassword.isEmpty()) {
      return false;
    }

    final String sha1 = sha1Hex(rawPassword).toUpperCase();
    final String prefix = sha1.substring(0, 5);
    final String suffix = sha1.substring(5);

    try {
      String body = restClient.get()
          .uri("/range/{prefix}", prefix)
          // Padding hides the real result-set size from network observers.
          .header("Add-Padding", "true")
          .retrieve()
          .body(String.class);

      if (body == null) return false;

      for (String line : body.split("\\r?\\n")) {
        int colon = line.indexOf(':');
        if (colon <= 0) continue;
        String candidateSuffix = line.substring(0, colon).trim();
        if (candidateSuffix.equalsIgnoreCase(suffix)) {
          // Padding entries have a count of 0 — ignore those.
          String count = line.substring(colon + 1).trim();
          if (!"0".equals(count)) {
            return true;
          }
        }
      }
      return false;
    } catch (Exception e) {
      // Fail open: never block a legitimate user because HIBP is down.
      log.warn("Pwned-password check unavailable, allowing password: {}", e.getClass().getSimpleName());
      return false;
    }
  }

  private static String sha1Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16));
        sb.append(Character.forDigit((b & 0xF), 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-1 not available", e);
    }
  }
}
