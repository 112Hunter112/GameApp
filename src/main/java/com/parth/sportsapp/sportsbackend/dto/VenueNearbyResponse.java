package com.parth.sportsapp.sportsbackend.dto;

import java.util.UUID;

public class VenueNearbyResponse {
  private UUID id;
  private String name;
  private Double distanceMeters;

  public VenueNearbyResponse() {}

  public VenueNearbyResponse(UUID id, String name, Double distanceMeters) {
    this.id = id;
    this.name = name;
    this.distanceMeters = distanceMeters;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public Double getDistanceMeters() { return distanceMeters; }
  public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }
}
