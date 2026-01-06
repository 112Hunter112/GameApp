package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.FriendshipStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class FriendshipResponse {

  private UUID id;                    // The Friendship ID
  private FriendshipStatus status;    // PENDING, ACCEPTED
  private LocalDateTime createdAt;    // "Sent 2 hours ago"

  // We send BOTH parties so the frontend knows who is who
  private UserSummaryDto requester;
  private UserSummaryDto receiver;

  // --- Getters & Setters ---
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public FriendshipStatus getStatus() { return status; }
  public void setStatus(FriendshipStatus status) { this.status = status; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public UserSummaryDto getRequester() { return requester; }
  public void setRequester(UserSummaryDto requester) { this.requester = requester; }

  public UserSummaryDto getReceiver() { return receiver; }
  public void setReceiver(UserSummaryDto receiver) { this.receiver = receiver; }
}
