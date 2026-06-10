package com.parth.sportsapp.sportsbackend.dto;

import java.time.LocalDateTime;

/**
 * One cell of the availability grid, sized by the venue's slot increment.
 * The frontend composes longer sessions out of consecutive available cells.
 */
public class TimeSlotDto {

  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private boolean available;

  public TimeSlotDto() {}

  public TimeSlotDto(LocalDateTime startTime, LocalDateTime endTime, boolean available) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.available = available;
  }

  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

  public boolean isAvailable() { return available; }
  public void setAvailable(boolean available) { this.available = available; }
}
