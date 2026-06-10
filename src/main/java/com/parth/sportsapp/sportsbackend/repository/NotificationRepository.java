package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Notification;
import com.parth.sportsapp.sportsbackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  // Get my notifications (Newest first)
  List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

  // Get count of unread messages (For the red badge 🔴 on the frontend)
  long countByRecipientAndIsReadFalse(User recipient);

  List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

  /** Paged feed for the notifications screen. */
  Page<Notification> findByRecipient_IdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

  /** Bell badge count — polled by the app, so keep it a bare COUNT. */
  long countByRecipient_IdAndIsReadFalse(UUID recipientId);

  /** "Mark all read" in one statement instead of N saves. */
  @Modifying
  @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :userId AND n.isRead = false")
  int markAllReadForUser(@Param("userId") UUID userId);
}
