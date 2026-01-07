package com.parth.sportsapp.sportsbackend.model;

public enum MatchSource {
  APP_BOOKING,   // Created automatically via a Venue Booking
  MANUAL_ENTRY,  // "I played at the park"
  EXTERNAL       // Imported from another app
}
