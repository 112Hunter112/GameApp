package com.parth.sportsapp.sportsbackend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** One card in venue search results — everything the list UI needs in one object. */
public class VenueSearchResult {

  private UUID id;
  private String name;
  private String address;

  /** Metres from the searcher; null when they didn't share a location. */
  private Double distanceMeters;

  /** Distinct sports playable here ("Tennis", "Badminton"...). */
  private List<String> sports;

  private int courtCount;

  /** Cheapest active court, for a "from £12/hr" label. Null if no priced courts. */
  private BigDecimal minHourlyRate;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }

  public Double getDistanceMeters() { return distanceMeters; }
  public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }

  public List<String> getSports() { return sports; }
  public void setSports(List<String> sports) { this.sports = sports; }

  public int getCourtCount() { return courtCount; }
  public void setCourtCount(int courtCount) { this.courtCount = courtCount; }

  public BigDecimal getMinHourlyRate() { return minHourlyRate; }
  public void setMinHourlyRate(BigDecimal minHourlyRate) { this.minHourlyRate = minHourlyRate; }
}
