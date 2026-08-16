package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    // The bell badge (unread count) is polled by every client; the feed pages
    // by recipient newest-first. Both must be index hits, not table scans —
    // this table only ever grows.
    @Index(name = "idx_notification_recipient_unread", columnList = "recipient_id, is_read"),
    @Index(name = "idx_notification_recipient_created", columnList = "recipient_id, created_at")
})
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "recipient_id", nullable = false)
  private User recipient; // Who sees this notification

  @ManyToOne
  @JoinColumn(name = "sender_id")
  private User sender; // Who triggered it (optional, e.g., System or Parth)

  @Column(nullable = false)
  private String type; // "MATCH_INVITE", "FRIEND_REQUEST", "SYSTEM_ALERT"

  private UUID referenceId; // ID of the Match or Object to click on

  @Column(nullable = false)
  private String message; // "Parth logged a match result against you."

  private boolean isRead = false;

  private LocalDateTime createdAt = LocalDateTime.now();

  // --- CONSTRUCTORS ---
  public Notification() {}

  // --- GETTERS AND SETTERS ---
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public User getRecipient() { return recipient; }
  public void setRecipient(User recipient) { this.recipient = recipient; }

  public User getSender() { return sender; }
  public void setSender(User sender) { this.sender = sender; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  public UUID getReferenceId() { return referenceId; }
  public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }

  public boolean isRead() { return isRead; }
  public void setRead(boolean read) { isRead = read; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
