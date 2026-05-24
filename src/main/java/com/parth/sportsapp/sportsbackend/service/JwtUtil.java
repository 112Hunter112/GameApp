package com.parth.sportsapp.sportsbackend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtUtil {

  private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

  /** Minimum bytes for HS256. 32 bytes = 256 bits. */
  private static final int MIN_KEY_BYTES = 32;

  @Value("${jwt.secret}")
  private String SECRET_KEY;

  @Value("${jwt.expiration}")
  private long EXPIRATION_TIME;

  private Key signingKey;

  /**
   * Validated at boot. Fails fast if the secret is missing, malformed, or too
   * short to be secure. Prevents a silent vulnerability where someone deploys
   * with a 4-byte demo key.
   */
  @PostConstruct
  void init() {
    if (SECRET_KEY == null || SECRET_KEY.isBlank()) {
      throw new IllegalStateException("jwt.secret is not configured");
    }
    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(SECRET_KEY);
    } catch (Exception e) {
      throw new IllegalStateException("jwt.secret must be Base64 encoded", e);
    }
    if (keyBytes.length < MIN_KEY_BYTES) {
      throw new IllegalStateException(
          "jwt.secret must decode to at least " + MIN_KEY_BYTES + " bytes (256 bits) for HS256");
    }
    if (EXPIRATION_TIME <= 0) {
      throw new IllegalStateException("jwt.expiration must be a positive number of milliseconds");
    }
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    log.info("JWT signing key initialized ({} bits, expiry {} ms)", keyBytes.length * 8, EXPIRATION_TIME);
  }

  public String generateToken(String email, UUID userId, String role) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId.toString());
    claims.put("role", "ROLE_" + role);
    return createToken(claims, email);
  }

  public String createToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }

  /** Kept for any external callers — uses the cached signing key. */
  public Key getSigningKey() {
    return signingKey;
  }

  public String extractUserId(String token) {
    return extractClaim(token, claims -> claims.get("userId", String.class));
  }

  public String extractRole(String token) {
    return extractClaim(token, claims -> claims.get("role", String.class));
  }

  public String extractEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(signingKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private boolean isTokenExpired(String token) {
    try {
      return extractExpiration(token).before(new Date());
    } catch (JwtException e) {
      return true;
    }
  }

  /** True if signature is valid, subject matches, and the token is not expired. */
  public boolean validateToken(String token, String email) {
    try {
      final String tokenEmail = extractEmail(token);
      return tokenEmail != null && tokenEmail.equals(email) && !isTokenExpired(token);
    } catch (JwtException e) {
      // Invalid signature, malformed token, etc. — never log the token itself.
      log.debug("Token validation failed: {}", e.getClass().getSimpleName());
      return false;
    }
  }
}
