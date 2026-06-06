package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.AuthResponse;
import com.parth.sportsapp.sportsbackend.dto.GoogleLoginRequest;
import com.parth.sportsapp.sportsbackend.dto.LoginRequest;
import com.parth.sportsapp.sportsbackend.dto.RefreshTokenRequest;
import com.parth.sportsapp.sportsbackend.dto.RegisterRequest;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.AuthService;
import com.parth.sportsapp.sportsbackend.service.GoogleAuthService;
import com.parth.sportsapp.sportsbackend.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// CORS is handled centrally in SecurityConfig (app.cors.allowed-origins).
// Do NOT add @CrossOrigin here — it would override the central policy.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired
  private AuthService authService;

  @Autowired
  private GoogleAuthService googleAuthService;

  @Autowired
  private RefreshTokenService refreshTokenService;



  /**
   * // we use the dto as the paramter here, but it is made into a java object using RequestBody
   *     // which makes from JSON to java object as requested
   *     //register the user, this will insert the dat into the table
   *   // we use the dto as the paramter here, but it is made into a java object using RequestBody
   *   // which makes from JSON to java object as requested
   *This will be accessible at: POST http://localhost:8080/auth/register
   * @param registerRequest holds data of user from frontend
   * @return
   */
  @PostMapping("/register") // put data on the database
  public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {

    try {
      //authService takes the input from frontned as registerRequest and makes it into output as AuthResponse
      String authResponse = authService.register(registerRequest); //

      return ResponseEntity.ok(authResponse);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }

  }

  // This will be accessible at: POST http://localhost:8080/auth/login
  //? tells the user that th personmigth return a random variable String(for error) or AuthResponse
  @PostMapping("/login")
  public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {

    try {
      AuthResponse authResponse = authService.login(loginRequest);
      return ResponseEntity.ok(authResponse);
    } catch (RuntimeException e) {
      // Now "User not found" or "Not Verified" returns 400 Bad Request, not 500 Crash
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }


  @GetMapping("/verify")
  public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
    try {
      String result = authService.verifyAccount(token);
      return ResponseEntity.ok(result);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  /**
   * Google Sign-In. The mobile app obtains a Google ID token via the Google Sign-In
   * SDK and POSTs it here. The backend verifies the signature against Google's keys,
   * finds or creates the matching user, and returns our own JWT.
   *
   * Body: { "idToken": "<google id token>" }
   */
  @PostMapping("/google")
  public ResponseEntity<AuthResponse> loginWithGoogle(
      @Valid @RequestBody GoogleLoginRequest request) {
    return ResponseEntity.ok(googleAuthService.loginWithGoogle(request.getIdToken()));
  }

  /**
   * Exchange a valid refresh token for a fresh access token + a new (rotated)
   * refresh token. The old refresh token is invalidated.
   *
   * Body: { "refreshToken": "<token>" }
   */
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(refreshTokenService.refresh(request.getRefreshToken()));
  }

  /**
   * Logout on this device: revoke the supplied refresh token. The access token
   * remains valid until it expires (max 15 min), but it can no longer be renewed.
   *
   * Body: { "refreshToken": "<token>" }
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    refreshTokenService.revoke(request.getRefreshToken());
    return ResponseEntity.noContent().build();
  }

  /**
   * Revoke EVERY refresh token for the authenticated user — "log me out on all
   * devices." Used after password change, suspected token theft, or the user
   * tapping "Sign out everywhere" in settings.
   *
   * Existing access tokens stay valid until they expire (max 15 min), but they
   * can no longer be renewed, so the user is effectively kicked off within
   * minutes on every device.
   */
  @PostMapping("/logout-all")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> logoutEverywhere(@AuthenticationPrincipal User user) {
    refreshTokenService.revokeAllForUser(user.getId());
    return ResponseEntity.noContent().build();
  }
}
