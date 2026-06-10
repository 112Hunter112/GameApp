package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Owner request to block out time on a court. */
public class CourtBlockRequest {

  @NotNull(message = "startTime is required")
  private LocalDateTime startTime;

  @NotNull(message = "endTime is required")
  @Future(message = "endTime must be in the future")
  private LocalDateTime endTime;

  @Size(max = 200, message = "reason must be at most 200 characters")
  private String reason;

  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
}
