package com.parth.sportsapp.sportsbackend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CourtResponse {

  private UUID id;            // The DB ID of this court
  private UUID venueId;       // Which venue it belongs to

  // Flattened Sport Details (Easier for Frontend to display)
  private UUID sportId;
  private String sportName;

  // Core Details
  private String courtNumber;
  private BigDecimal hourlyRate;
  private boolean isIndoor;
  private String surfaceType;
  private Integer capacity;
  private List<String> amenities;

  // ==========================
  // GETTERS & SETTERS
  // ==========================

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getVenueId() { return venueId; }
  public void setVenueId(UUID venueId) { this.venueId = venueId; }

  public UUID getSportId() { return sportId; }
  public void setSportId(UUID sportId) { this.sportId = sportId; }

  public String getSportName() { return sportName; }
  public void setSportName(String sportName) { this.sportName = sportName; }

  public String getCourtNumber() { return courtNumber; }
  public void setCourtNumber(String courtNumber) { this.courtNumber = courtNumber; }

  public BigDecimal getHourlyRate() { return hourlyRate; }
  public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

  public boolean isIndoor() { return isIndoor; }
  public void setIndoor(boolean indoor) { isIndoor = indoor; }

  public String getSurfaceType() { return surfaceType; }
  public void setSurfaceType(String surfaceType) { this.surfaceType = surfaceType; }

  public Integer getCapacity() { return capacity; }
  public void setCapacity(Integer capacity) { this.capacity = capacity; }

  public List<String> getAmenities() { return amenities; }
  public void setAmenities(List<String> amenities) { this.amenities = amenities; }
}
