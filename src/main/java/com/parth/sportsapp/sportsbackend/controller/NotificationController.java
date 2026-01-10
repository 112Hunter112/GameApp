package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.model.Notification;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.NotificationRepository;
import com.parth.sportsapp.sportsbackend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private NotificationService notificationService;

  // 1. Get My Notifications (The "Check Mailbox" endpoint)
  @GetMapping
  public ResponseEntity<List<Notification>> getMyNotifications() {
    UUID userId = getCurrentUserId();
    // Fetch unread notifications first, or all recent ones
    List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    return ResponseEntity.ok(notifications);
  }

  // 2. Mark as Read (When they click it)
  @PutMapping("/{id}/read")
  public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
    notificationService.markAsRead(id);
    return ResponseEntity.ok().build();
  }
// todo fix this method

//  // 3. Count Unread (For the little red badge on the bell icon 🔔)
//  @GetMapping("/unread-count")
//  public ResponseEntity<Long> getUnreadCount() {
//    UUID userId = getCurrentUserId();
//    long count = notificationRepository.countByRecipientIdAndIsReadFalse(userId);
//    return ResponseEntity.ok(count);
//  }

  // Helper to get User ID
  private UUID getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    User user = (User) auth.getPrincipal();
    return user.getId();
  }
}
