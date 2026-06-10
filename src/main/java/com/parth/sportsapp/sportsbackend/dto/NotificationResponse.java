package com.parth.sportsapp.sportsbackend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row in the notification list. Carries enough for the app to render the
 * card AND deep-link: the frontend maps {type, referenceId} to a destination.
 */
public class NotificationResponse {

  private UUID id;
  private String type;          // BOOKING_REQUESTED, FRIEND_REQUEST, MATCH_INVITE, ...
  private String message;
  private UUID referenceId;     // bookingId / friendshipId / matchId, per type
  private boolean read;
  private LocalDateTime createdAt;

  // Who triggered it (null for system notifications like BOOKING_CONFIRMED)
  private UUID senderId;
  private String senderName;
  private String senderAvatarUrl;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }

  public UUID getReferenceId() { return referenceId; }
  public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

  public boolean isRead() { return read; }
  public void setRead(boolean read) { this.read = read; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public UUID getSenderId() { return senderId; }
  public void setSenderId(UUID senderId) { this.senderId = senderId; }

  public String getSenderName() { return senderName; }
  public void setSenderName(String senderName) { this.senderName = senderName; }

  public String getSenderAvatarUrl() { return senderAvatarUrl; }
  public void setSenderAvatarUrl(String senderAvatarUrl) { this.senderAvatarUrl = senderAvatarUrl; }
}
