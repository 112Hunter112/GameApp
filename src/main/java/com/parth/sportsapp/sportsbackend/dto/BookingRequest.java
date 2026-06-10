package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** Player request to book a court. Times are venue-local. */
public class BookingRequest {

  @NotNull(message = "courtId is required")
  private UUID courtId;

  @NotNull(message = "startTime is required")
  @Future(message = "startTime must be in the future")
  private LocalDateTime startTime;

  @NotNull(message = "endTime is required")
  private LocalDateTime endTime;

  @Size(max = 500, message = "notes must be at most 500 characters")
  private String notes;

  public UUID getCourtId() { return courtId; }
  public void setCourtId(UUID courtId) { this.courtId = courtId; }

  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}
