package com.parth.sportsapp.sportsbackend.model;

public enum BookingStatus {
  PENDING,
  CONFIRMED,
  CANCELLED,
  COMPLETED,
  /** Marked by the venue owner: player booked, never showed. Feeds reliability scoring. */
  NO_SHOW
}
