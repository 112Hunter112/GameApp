package com.parth.sportsapp.sportsbackend.model;

public enum FriendshipStatus {
  PENDING,    // Request sent, waiting for action
  ACCEPTED,   // Friends!
  DECLINED,   // Receiver said no
  BLOCKED     // Requester cannot contact Receiver
}
