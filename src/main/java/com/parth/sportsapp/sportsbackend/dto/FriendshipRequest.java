package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * This is responsible for the input fo info that is the bare minimum so the user cant mess with
 * the other data such as STATUS, or other things
 */
public class FriendshipRequest {


  // we need the userID which is from user to see who the request is going to
  @NotNull(message = "receiverId is required")
  private UUID receiverId;

  @Size(max = 500, message = "Message cannot exceed 500 characters")
  private String message;

  // -GETTERS and SETTERS
  public UUID getReceiverId() {
    return receiverId;
  }

  public void setReceiverId(UUID receiverId) {
    this.receiverId = receiverId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }



}
