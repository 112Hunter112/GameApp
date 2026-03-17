package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.VenueSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VenueResponse {

  private UUID id;
  private String name;
  private String address;
  private String phoneNumber;
  private String description;

  // Flattened location for easy map plotting
  private Double latitude;
  private Double longitude;

  private Map<String, Object> openingHours;
  private List<String> amenities;
  private boolean isActive;

  // SAFE Owner Info (We don't return the full User entity with password!)
  private OwnerSummaryDto owner;

  private String externalId;
  private VenueSource source;

  // --- Nested DTO for Owner ---
  public static class OwnerSummaryDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;

    // Constructor, Getters, Setters
    public OwnerSummaryDto(UUID id, String firstName, String lastName, String email) {
      this.id = id;
      this.firstName = firstName;
      this.lastName = lastName;
      this.email = email;
    }
    // ... getters ...
  }

  // --- Getters & Setters for VenueResponseDto ---
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getPhoneNumber() { return phoneNumber; }
  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Double getLatitude() { return latitude; }
  public void setLatitude(Double latitude) { this.latitude = latitude; }
  public Double getLongitude() { return longitude; }
  public void setLongitude(Double longitude) { this.longitude = longitude; }
  public Map<String, Object> getOpeningHours() { return openingHours; }
  public void setOpeningHours(Map<String, Object> openingHours) { this.openingHours = openingHours; }
  public List<String> getAmenities() { return amenities; }
  public void setAmenities(List<String> amenities) { this.amenities = amenities; }
  public boolean isActive() { return isActive; }
  public void setActive(boolean active) { isActive = active; }
  public OwnerSummaryDto getOwner() { return owner; }
  public void setOwner(OwnerSummaryDto owner) { this.owner = owner; }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public VenueSource getSource() {
    return source;
  }

  public void setSource(VenueSource source) {
    this.source = source;
  }
}
