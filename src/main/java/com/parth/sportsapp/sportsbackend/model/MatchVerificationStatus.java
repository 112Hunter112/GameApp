package com.parth.sportsapp.sportsbackend.model;


public enum MatchVerificationStatus {
  PENDING,    // User A submitted it, User B hasn't seen it
  CONFIRMED,  // User B accepted the result
  DISPUTED,   // User B says "That score is wrong"
  REJECTED    // User B says "We never played"
}
