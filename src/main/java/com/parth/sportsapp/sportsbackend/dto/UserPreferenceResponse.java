package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.Gender;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What the API returns for a single user preference. Sport details are flattened
 * (sportId + sportName) so the frontend doesn't need a second call to /api/sports.
 */
public class UserPreferenceResponse {

  private UUID sportId;
  private String sportName;

  private String proficiencyLevel;
  private Integer eloRating;
  private Integer matchesPlayed;
  private Integer matchesWon;

  private Integer yearsPlaying;
  private String preferredFormat;
  private Gender preferredOpponentGender;
  private Integer minOpponentAge;
  private Integer maxOpponentAge;
  private Integer minOpponentElo;
  private Integer maxOpponentElo;
  private Integer maxTravelDistanceMeters;
  private Boolean openToMatchmaking;
  private Boolean availableWeekdays;
  private Boolean availableWeekends;
  private Integer notificationRadiusMeters;
  private Boolean isPrimarySport;

  private LocalDateTime lastActiveAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public UserPreferenceResponse() {}

  public UUID getSportId() { return sportId; }
  public void setSportId(UUID v) { this.sportId = v; }

  public String getSportName() { return sportName; }
  public void setSportName(String v) { this.sportName = v; }

  public String getProficiencyLevel() { return proficiencyLevel; }
  public void setProficiencyLevel(String v) { this.proficiencyLevel = v; }

  public Integer getEloRating() { return eloRating; }
  public void setEloRating(Integer v) { this.eloRating = v; }

  public Integer getMatchesPlayed() { return matchesPlayed; }
  public void setMatchesPlayed(Integer v) { this.matchesPlayed = v; }

  public Integer getMatchesWon() { return matchesWon; }
  public void setMatchesWon(Integer v) { this.matchesWon = v; }

  public Integer getYearsPlaying() { return yearsPlaying; }
  public void setYearsPlaying(Integer v) { this.yearsPlaying = v; }

  public String getPreferredFormat() { return preferredFormat; }
  public void setPreferredFormat(String v) { this.preferredFormat = v; }

  public Gender getPreferredOpponentGender() { return preferredOpponentGender; }
  public void setPreferredOpponentGender(Gender v) { this.preferredOpponentGender = v; }

  public Integer getMinOpponentAge() { return minOpponentAge; }
  public void setMinOpponentAge(Integer v) { this.minOpponentAge = v; }

  public Integer getMaxOpponentAge() { return maxOpponentAge; }
  public void setMaxOpponentAge(Integer v) { this.maxOpponentAge = v; }

  public Integer getMinOpponentElo() { return minOpponentElo; }
  public void setMinOpponentElo(Integer v) { this.minOpponentElo = v; }

  public Integer getMaxOpponentElo() { return maxOpponentElo; }
  public void setMaxOpponentElo(Integer v) { this.maxOpponentElo = v; }

  public Integer getMaxTravelDistanceMeters() { return maxTravelDistanceMeters; }
  public void setMaxTravelDistanceMeters(Integer v) { this.maxTravelDistanceMeters = v; }

  public Boolean getOpenToMatchmaking() { return openToMatchmaking; }
  public void setOpenToMatchmaking(Boolean v) { this.openToMatchmaking = v; }

  public Boolean getAvailableWeekdays() { return availableWeekdays; }
  public void setAvailableWeekdays(Boolean v) { this.availableWeekdays = v; }

  public Boolean getAvailableWeekends() { return availableWeekends; }
  public void setAvailableWeekends(Boolean v) { this.availableWeekends = v; }

  public Integer getNotificationRadiusMeters() { return notificationRadiusMeters; }
  public void setNotificationRadiusMeters(Integer v) { this.notificationRadiusMeters = v; }

  public Boolean getIsPrimarySport() { return isPrimarySport; }
  public void setIsPrimarySport(Boolean v) { this.isPrimarySport = v; }

  public LocalDateTime getLastActiveAt() { return lastActiveAt; }
  public void setLastActiveAt(LocalDateTime v) { this.lastActiveAt = v; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}
