package com.parth.sportsapp.sportsbackend.dto;

import java.util.UUID;

/**
 * Lightweight sport-chip data for the home screen. Lets the frontend render
 * a "Your sports" row and a "New sports nearby" row without re-fetching the
 * Sports endpoint.
 */
public class SportSummary {

  private UUID sportId;
  private String sportName;
  private String iconUrl;
  /** Present only on the user's own preferred sports (null in the discovery list). */
  private Integer eloRating;
  /** True on the user's marked-primary sport (null otherwise). */
  private Boolean isPrimary;

  public SportSummary() {}

  public SportSummary(UUID sportId, String sportName, String iconUrl,
                      Integer eloRating, Boolean isPrimary) {
    this.sportId = sportId;
    this.sportName = sportName;
    this.iconUrl = iconUrl;
    this.eloRating = eloRating;
    this.isPrimary = isPrimary;
  }

  public UUID getSportId() { return sportId; }
  public void setSportId(UUID v) { this.sportId = v; }

  public String getSportName() { return sportName; }
  public void setSportName(String v) { this.sportName = v; }

  public String getIconUrl() { return iconUrl; }
  public void setIconUrl(String v) { this.iconUrl = v; }

  public Integer getEloRating() { return eloRating; }
  public void setEloRating(Integer v) { this.eloRating = v; }

  public Boolean getIsPrimary() { return isPrimary; }
  public void setIsPrimary(Boolean v) { this.isPrimary = v; }
}
