package com.parth.sportsapp.sportsbackend.dto;

import java.util.UUID;

/**
 * This is responsible for the input fo info that is the bare minimum so the user cant mess with
 * the other data such as STATUS, or other things
 */
public class FriendshipRequest {


  // we need the userID which is from user to see who the request is going to
  private UUID receiverId;

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
