package com.parth.sportsapp.sportsbackend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class CourtBlockResponse {

  private UUID id;
  private UUID courtId;
  private String courtNumber;
  private UUID venueId;
  private String venueName;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String reason;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getCourtId() { return courtId; }
  public void setCourtId(UUID courtId) { this.courtId = courtId; }

  public String getCourtNumber() { return courtNumber; }
  public void setCourtNumber(String courtNumber) { this.courtNumber = courtNumber; }

  public UUID getVenueId() { return venueId; }
  public void setVenueId(UUID venueId) { this.venueId = venueId; }

  public String getVenueName() { return venueName; }
  public void setVenueName(String venueName) { this.venueName = venueName; }

  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
}
