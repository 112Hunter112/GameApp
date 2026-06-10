package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * This dictates what info we receive from the user input
 *
 * When the person presses the book button, the info that goes to the user is
 * CourtID, StartTime, EndTime
 */
public class BookingRequest {

  @NotNull(message = "Court ID is required")
  private UUID courtId;


  @NotNull(message = "Start time is required")
  @Future(message = "Booking must be in the future")
  private LocalDate startTime;

  @NotNull(message = "End time is required")
  @Future(message = "Booking must be in the future")
  private LocalDate endTime;


  //user sets if they want to book out the court for the whole period
  public boolean isPrivate = false;

  // todo : verify this ot be true only if person has chosen IsPriavte to true, if this is the
  //  case then set default player limit unless specified
  private Integer playerLimit;

  public UUID getCourtId() {
    return courtId;
  }

  public void setCourtId(UUID courtId) {
    this.courtId = courtId;
  }

  public LocalDate getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDate startTime) {
    this.startTime = startTime;
  }

  public LocalDate getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDate endTime) {
    this.endTime = endTime;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public void setPrivate(boolean aPrivate) {
    isPrivate = aPrivate;
  }

  public Integer getPlayerLimit() {
    return playerLimit;
  }

  public void setPlayerLimit(Integer playerLimit) {
    this.playerLimit = playerLimit;
  }
}
