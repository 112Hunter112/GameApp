package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.UserPreferenceRequest;
import com.parth.sportsapp.sportsbackend.dto.UserPreferenceResponse;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Per-user preference CRUD. Path is /api/me/preferences — there is no
 * /api/users/{id}/preferences variant, so a user cannot read or mutate
 * another user's preferences.
 */
@RestController
@RequestMapping("/api/me/preferences")
@Validated
@PreAuthorize("isAuthenticated()")
public class UserPreferenceController {

  private final UserPreferenceService service;

  public UserPreferenceController(UserPreferenceService service) {
    this.service = service;
  }

  @GetMapping
  public List<UserPreferenceResponse> list(@AuthenticationPrincipal User user) {
    return service.listMine(user.getId());
  }

  @GetMapping("/{sportId}")
  public UserPreferenceResponse get(@AuthenticationPrincipal User user,
                                    @PathVariable UUID sportId) {
    return service.get(user.getId(), sportId);
  }

  /** Create-or-update. Idempotent for the same (user, sport) pair. */
  @PutMapping
  public UserPreferenceResponse upsert(@AuthenticationPrincipal User user,
                                       @Valid @RequestBody UserPreferenceRequest request) {
    return service.upsert(user.getId(), request);
  }

  @PostMapping("/{sportId}/primary")
  public ResponseEntity<Void> setPrimary(@AuthenticationPrincipal User user,
                                         @PathVariable UUID sportId) {
    service.setPrimary(user.getId(), sportId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{sportId}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                     @PathVariable UUID sportId) {
    service.delete(user.getId(), sportId);
    return ResponseEntity.noContent().build();
  }
}
