package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.NotificationResponse;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * In-app notification feed.
 *
 *   GET  /api/notifications?page=&size=   paged feed (DTOs, newest first)
 *   GET  /api/notifications/unread-count  bell badge ({"count": n})
 *   PUT  /api/notifications/{id}/read     mark one read (on tap)
 *   POST /api/notifications/read-all      mark everything read
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  @Autowired
  private NotificationService notificationService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
      @AuthenticationPrincipal User user,
      Pageable pageable) {
    return ResponseEntity.ok(notificationService.getMyNotifications(user.getId(), pageable));
  }

  @GetMapping("/unread-count")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user.getId())));
  }

  @PutMapping("/{id}/read")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> markAsRead(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id) {
    notificationService.markAsRead(id, user.getId());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/read-all")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Map<String, Integer>> markAllAsRead(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(Map.of("updated", notificationService.markAllAsRead(user.getId())));
  }
}
