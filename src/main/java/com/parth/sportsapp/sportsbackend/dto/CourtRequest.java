package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CourtRequest {

  @NotBlank(message = "Court name or number is required")
  private String courtNumber; // e.g., "Court 1", "Center Court"

  @NotNull(message = "Sport ID is required")
  private UUID sportId; // The ID of the sport this court is for (e.g., Tennis ID)

  @NotNull(message = "Hourly rate is required")
  @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
  private BigDecimal hourlyRate;

  private boolean isIndoor; // true = Indoor, false = Outdoor

  private String surfaceType; // e.g., "Grass", "Clay", "Hard Court", "Synthetic"

  private Integer capacity; // How many players/spectators fits?

  private List<String> amenities; // e.g., ["Lights", "Umpire Chair", "Covered"]

  // ==========================
  // GETTERS & SETTERS
  // ==========================

  public String getCourtNumber() {
    return courtNumber;
  }

  public void setCourtNumber(String courtNumber) {
    this.courtNumber = courtNumber;
  }

  public UUID getSportId() {
    return sportId;
  }

  public void setSportId(UUID sportId) {
    this.sportId = sportId;
  }

  public BigDecimal getHourlyRate() {
    return hourlyRate;
  }

  public void setHourlyRate(BigDecimal hourlyRate) {
    this.hourlyRate = hourlyRate;
  }

  public boolean getIsIndoor() {
    return isIndoor;
  }

  public void setIsIndoor(boolean isIndoor) {
    this.isIndoor = isIndoor;
  }

  public String getSurfaceType() {
    return surfaceType;
  }

  public void setSurfaceType(String surfaceType) {
    this.surfaceType = surfaceType;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public List<String> getAmenities() {
    return amenities;
  }

  public void setAmenities(List<String> amenities) {
    this.amenities = amenities;
  }
}
