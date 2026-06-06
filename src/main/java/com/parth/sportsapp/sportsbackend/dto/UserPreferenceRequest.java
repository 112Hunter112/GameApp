package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Body of {@code PUT /api/me/preferences}. Identifies the sport (required) and
 * carries every editable field. {@code eloRating}, {@code matchesPlayed},
 * {@code matchesWon}, and {@code isPrimarySport} are intentionally NOT editable
 * here — Elo is maintained by the match flow, primary is set via a dedicated
 * endpoint.
 */
public class UserPreferenceRequest {

  @NotNull(message = "sportId is required")
  private UUID sportId;

  @Size(max = 20)
  private String proficiencyLevel;            // BEGINNER | INTERMEDIATE | ADVANCED | EXPERT

  @Min(0) @Max(80)
  private Integer yearsPlaying;

  @Size(max = 20)
  private String preferredFormat;             // SINGLES | DOUBLES | TEAM | MIXED

  private Gender preferredOpponentGender;

  @Min(13) @Max(99)
  private Integer minOpponentAge;
  @Min(13) @Max(99)
  private Integer maxOpponentAge;

  @Min(0) @Max(4000)
  private Integer minOpponentElo;
  @Min(0) @Max(4000)
  private Integer maxOpponentElo;

  @Min(0) @Max(200_000)
  private Integer maxTravelDistanceMeters;

  private Boolean openToMatchmaking;
  private Boolean availableWeekdays;
  private Boolean availableWeekends;

  @Min(0) @Max(200_000)
  private Integer notificationRadiusMeters;

  public UUID getSportId() { return sportId; }
  public void setSportId(UUID sportId) { this.sportId = sportId; }

  public String getProficiencyLevel() { return proficiencyLevel; }
  public void setProficiencyLevel(String v) { this.proficiencyLevel = v; }

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
}
