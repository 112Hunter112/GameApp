package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.FeedItemResponse;
import com.parth.sportsapp.sportsbackend.dto.FeedPageResponse;
import com.parth.sportsapp.sportsbackend.dto.FeedShareRequest;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.FeedShareService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The social feed: what my friends shared, newest first, plus share/unshare.
 * Reads are Redis-cached per viewer with a short TTL (see FeedShareService
 * for the exact freshness/deletion semantics).
 */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

  private final FeedShareService feedShareService;

  public FeedController(FeedShareService feedShareService) {
    this.feedShareService = feedShareService;
  }

  /** One feed page (20 items). Pages beyond the cached window hit the DB. */
  @GetMapping
  public ResponseEntity<FeedPageResponse> feed(@AuthenticationPrincipal User user,
                                               @RequestParam(defaultValue = "0") int page) {
    return ResponseEntity.ok(feedShareService.getFeed(user.getId(), page));
  }

  /** Share a match I played to my feed. */
  @PostMapping("/shares")
  public ResponseEntity<FeedItemResponse> share(@AuthenticationPrincipal User user,
                                                @Valid @RequestBody FeedShareRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(feedShareService.share(user.getId(), request));
  }

  /** Remove my share. Viewers' cached pages age out within the cache TTL. */
  @DeleteMapping("/shares/{shareId}")
  public ResponseEntity<Void> unshare(@AuthenticationPrincipal User user,
                                      @PathVariable UUID shareId) {
    feedShareService.unshare(shareId, user.getId());
    return ResponseEntity.noContent().build();
  }
}
