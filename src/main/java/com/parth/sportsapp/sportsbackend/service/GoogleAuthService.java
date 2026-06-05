package com.parth.sportsapp.sportsbackend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.parth.sportsapp.sportsbackend.dto.AuthResponse;
import com.parth.sportsapp.sportsbackend.exception.UnauthorizedException;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserRole;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

/**
 * Verifies a Google Sign-In ID token coming from a mobile client and either
 * logs the matching user in or creates a new account on first sign-in.
 *
 * <p>The mobile app uses Google's native Sign-In SDK to obtain an ID token,
 * then POSTs that token to {@code /api/auth/google}. We verify the token's
 * signature against Google's published JWKs and check that the audience
 * matches our configured client ID.
 */
@Service
public class GoogleAuthService {

  private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

  /** Configured at boot. Provided as one or more comma-separated client IDs. */
  @Value("${google.oauth.client-ids:}")
  private String clientIdsRaw;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final RefreshTokenService refreshTokenService;

  private GoogleIdTokenVerifier verifier;

  public GoogleAuthService(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           RefreshTokenService refreshTokenService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
    this.refreshTokenService = refreshTokenService;
  }

  @PostConstruct
  void init() {
    if (clientIdsRaw == null || clientIdsRaw.isBlank()) {
      // Don't fail boot — let the feature lazily error if someone tries to use it
      // without configuration. Logging instead so misconfig is visible.
      log.warn("google.oauth.client-ids is not configured. Google Sign-In will be unavailable.");
      return;
    }
    var audiences = java.util.Arrays.stream(clientIdsRaw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();

    this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
        .setAudience(audiences)
        .build();
    log.info("Google Sign-In enabled for {} client ID(s)", audiences.size());
  }

  /**
   * Verify a Google ID token, then return an AuthResponse containing our own JWT
   * for the matching (or newly created) user.
   */
  @Transactional
  public AuthResponse loginWithGoogle(String idTokenString) {
    if (verifier == null) {
      throw new UnauthorizedException("Google Sign-In is not configured on this server");
    }

    GoogleIdToken idToken;
    try {
      idToken = verifier.verify(idTokenString);
    } catch (Exception e) {
      log.debug("Google ID token verification failed: {}", e.getClass().getSimpleName());
      throw new UnauthorizedException("Invalid Google token");
    }
    if (idToken == null) {
      // Signature didn't match or audience was wrong.
      throw new UnauthorizedException("Invalid Google token");
    }

    Payload payload = idToken.getPayload();

    // Google requires verified emails; refuse otherwise.
    Boolean emailVerified = payload.getEmailVerified();
    if (emailVerified == null || !emailVerified) {
      throw new UnauthorizedException("Google account email is not verified");
    }

    String googleSub = payload.getSubject();
    String email = payload.getEmail();
    String firstName = stringClaim(payload, "given_name");
    String lastName = stringClaim(payload, "family_name");
    String pictureUrl = stringClaim(payload, "picture");

    if (googleSub == null || email == null) {
      throw new UnauthorizedException("Google token missing required claims");
    }

    // 1. If a user already has this googleId, log them in.
    User user = userRepository.findByGoogleId(googleSub).orElse(null);

    // 2. If not, but the email matches an existing account, link the google_id to it.
    //    (Same human, prior email/password signup, now using Google.)
    if (user == null) {
      user = userRepository.findByEmail(email).orElse(null);
      if (user != null) {
        user.setGoogleId(googleSub);
        userRepository.save(user);
      }
    }

    // 3. Otherwise create a brand-new user.
    if (user == null) {
      user = createGoogleUser(googleSub, email, firstName, lastName, pictureUrl);
    }

    String role = user.getRole() == null ? UserRole.USER.name() : user.getRole().name();
    String accessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), role);
    String refreshToken = refreshTokenService.issue(user.getId());

    return new AuthResponse(
        accessToken,
        refreshToken,
        jwtUtil.getAccessTokenExpiryMs() / 1000,
        user.getEmail(),
        user.getFirstName(),
        role
    );
  }

  // --- helpers -------------------------------------------------------------

  private User createGoogleUser(String googleSub, String email,
                                String firstName, String lastName, String pictureUrl) {
    User user = new User();
    user.setEmail(email);
    user.setFirstName(firstName == null ? "" : firstName);
    user.setLastName(lastName == null ? "" : lastName);
    user.setGoogleId(googleSub);
    user.setRole(UserRole.USER);
    // Email already verified by Google — no need for our own verification email.
    user.setVerified(true);
    user.setJoiningDate(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    if (pictureUrl != null) {
      user.setProfilePictureUrl(pictureUrl);
    }
    // Set a random unguessable password hash. The user can later set their own
    // via the change-password flow if they ever want to login by email/password.
    user.setPassword(passwordEncoder.encode(randomPlaceholderPassword()));
    return userRepository.save(user);
  }

  private static String randomPlaceholderPassword() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return UUID.nameUUIDFromBytes(bytes).toString();
  }

  private static String stringClaim(Payload payload, String key) {
    Object v = payload.get(key);
    return v == null ? null : v.toString();
  }
}
