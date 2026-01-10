package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Notification;
import com.parth.sportsapp.sportsbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
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


}
