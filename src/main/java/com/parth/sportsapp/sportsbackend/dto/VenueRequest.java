package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

public class VenueRequest {

  @NotBlank(message = "Venue name is required")
  private String name;

  @NotBlank(message = "Address is required")
  private String address;

  @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
  private String phoneNumber;

  private String description;

  // We accept simple doubles, not complex PostGIS Points
  @NotNull(message = "Latitude is required")
  @Min(-90) @Max(90)
  private Double latitude;

  @NotNull(message = "Longitude is required")
  @Min(-180) @Max(180)
  private Double longitude;

  @NotNull(message = "Opening hours are required")
  private Map<String, Object> openingHours;

  private List<String> amenities;

  // --- Getters & Setters ---
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
}
