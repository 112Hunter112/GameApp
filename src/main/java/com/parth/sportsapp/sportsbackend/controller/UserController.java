package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

  @Autowired
  private UserService userService;

  /**
   * This is the search bar for the user where they can find other players
   *
   * @param query
   * @return
   */
  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('USER', 'VENUE_OWNER')")
  public ResponseEntity<List<UserSummaryDto>> searchUsers(@RequestParam("query") String query) {
    if (query == null || query.trim().length() < 2) {
      return ResponseEntity.ok(List.of()); // Return empty if query is too short
    }
    return ResponseEntity.ok(userService.searchUsers(query));
  }



  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()") // <--- Allows USER, VENUE_OWNER, and ADMIN
  public ResponseEntity<UserSummaryDto> getCurrentUserProfile() {
    UUID userId = getCurrentUserId();
    // You will need to add 'getUserProfile' to your UserService
    return ResponseEntity.ok(userService.getUserProfile(userId));
  }

  private UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof User) {
      return ((User) authentication.getPrincipal()).getId();
    }
    throw new RuntimeException("User not authenticated");
  }
}
