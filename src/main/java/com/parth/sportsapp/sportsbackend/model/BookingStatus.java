package com.parth.sportsapp.sportsbackend.model;

public enum BookingStatus {
  /** Awaiting venue-owner approval (venues without instant-book). Holds the slot. */
  PENDING,
  /** Approved (or instant-booked). Holds the slot. */
  CONFIRMED,
  /** Venue owner rejected a PENDING request. Slot is freed. */
  DECLINED,
  /** Cancelled by player or owner before start. Slot is freed. */
  CANCELLED,
  /** A CONFIRMED booking whose end time has passed. */
  COMPLETED,
  /** Marked by the venue owner: player booked, never showed. Feeds reliability scoring. */
  NO_SHOW;

  /** Statuses that block the slot for other players. */
  public boolean blocksSlot() {
    return this == PENDING || this == CONFIRMED;
  }
}
