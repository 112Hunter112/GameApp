package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.ChangePasswordRequest;
import com.parth.sportsapp.sportsbackend.dto.PublicProfileResponse;
import com.parth.sportsapp.sportsbackend.dto.UpdateProfileRequest;
import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.service.JwtUtil;
import com.parth.sportsapp.sportsbackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

  @Autowired
  private UserService userService;

  @Autowired
  private JwtUtil jwtUtil;

  /**
   * Search bar for finding other players
   */
  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('USER', 'VENUE_OWNER')")
  public ResponseEntity<List<UserSummaryDto>> searchUsers(@RequestParam("query") String query) {
    if (query == null || query.trim().length() < 2) {
      return ResponseEntity.ok(List.of());
    }
    return ResponseEntity.ok(userService.searchUsers(query));
  }

  /**
   * Get current user's profile
   *
   * THIS WORKS
   */
  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserSummaryDto> getCurrentUserProfile(
      @RequestHeader("Authorization") String token) {

    UUID userId = getUserIdFromToken(token);
    return ResponseEntity.ok(userService.getUserProfile(userId));
  }

  /**
   * Extract user ID from JWT token (same pattern as your other controllers)
   */
  private UUID getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      String jwt = token.substring(7);
      return UUID.fromString(jwtUtil.extractUserId(jwt));
    }
    throw new RuntimeException("Invalid Token");
  }


  @GetMapping("/{userId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable UUID userId) {
    return ResponseEntity.ok(userService.getPublicProfile(userId));
  }

  @PutMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserSummaryDto> updateProfile(
      @RequestHeader("Authorization") String token,
      @Valid @RequestBody UpdateProfileRequest request) {

    UUID userId = getUserIdFromToken(token);
    return ResponseEntity.ok(userService.updateProfile(userId, request));
  }

  @PutMapping("/me/password")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<String> changePassword(
      @RequestHeader("Authorization") String token,
      @RequestBody ChangePasswordRequest request) {

    UUID userId = getUserIdFromToken(token);
    userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
    return ResponseEntity.ok("Password updated successfully");
  }
}
