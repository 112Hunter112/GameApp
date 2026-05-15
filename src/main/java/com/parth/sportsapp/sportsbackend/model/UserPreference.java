package com.parth.sportsapp.sportsbackend.model;


import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

  @EmbeddedId
  private UserPreferenceId id;

  @ManyToOne
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne
  @MapsId("sportId")
  @JoinColumn(name = "sport_id")
  private Sports sports;

  @Column(name = "proficiency_level")
  private String proficiencyLevel;

  @Column(name = "elo_rating", nullable = false)
  private Integer eloRating = 1200;

  @Column(name = "matches_played", nullable = false)
  private Integer matchesPlayed = 0;

  @Column(name = "matches_won", nullable = false)
  private Integer matchesWon = 0;

  @Column(name = "years_playing")
  private Integer yearsPlaying;

  @Column(name = "preferred_format")
  private String preferredFormat;

  @Enumerated(EnumType.STRING)
  @Column(name = "preferred_opponent_gender")
  private Gender preferredOpponentGender;

  @Column(name = "min_opponent_age")
  private Integer minOpponentAge;

  @Column(name = "max_opponent_age")
  private Integer maxOpponentAge;

  @Column(name = "min_opponent_elo")
  private Integer minOpponentElo;

  @Column(name = "max_opponent_elo")
  private Integer maxOpponentElo;

  @Column(name = "max_travel_distance_meters")
  private Integer maxTravelDistanceMeters;

  @Column(name = "open_to_matchmaking", nullable = false)
  private Boolean openToMatchmaking = true;

  @Column(name = "is_primary_sport", nullable = false)
  private Boolean isPrimarySport = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public UserPreferenceId getId() {
    return id;
  }

  public void setId(UserPreferenceId id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Sports getSports() {
    return sports;
  }

  public void setSports(Sports sports) {
    this.sports = sports;
  }

  public String getProficiencyLevel() {
    return proficiencyLevel;
  }

  public void setProficiencyLevel(String proficiencyLevel) {
    this.proficiencyLevel = proficiencyLevel;
  }

  public Integer getEloRating() {
    return eloRating;
  }

  public void setEloRating(Integer eloRating) {
    this.eloRating = eloRating;
  }

  public Integer getMatchesPlayed() {
    return matchesPlayed;
  }

  public void setMatchesPlayed(Integer matchesPlayed) {
    this.matchesPlayed = matchesPlayed;
  }

  public Integer getMatchesWon() {
    return matchesWon;
  }

  public void setMatchesWon(Integer matchesWon) {
    this.matchesWon = matchesWon;
  }

  public Integer getYearsPlaying() {
    return yearsPlaying;
  }

  public void setYearsPlaying(Integer yearsPlaying) {
    this.yearsPlaying = yearsPlaying;
  }

  public String getPreferredFormat() {
    return preferredFormat;
  }

  public void setPreferredFormat(String preferredFormat) {
    this.preferredFormat = preferredFormat;
  }

  public Gender getPreferredOpponentGender() {
    return preferredOpponentGender;
  }

  public void setPreferredOpponentGender(Gender preferredOpponentGender) {
    this.preferredOpponentGender = preferredOpponentGender;
  }

  public Integer getMinOpponentAge() {
    return minOpponentAge;
  }

  public void setMinOpponentAge(Integer minOpponentAge) {
    this.minOpponentAge = minOpponentAge;
  }

  public Integer getMaxOpponentAge() {
    return maxOpponentAge;
  }

  public void setMaxOpponentAge(Integer maxOpponentAge) {
    this.maxOpponentAge = maxOpponentAge;
  }

  public Integer getMinOpponentElo() {
    return minOpponentElo;
  }

  public void setMinOpponentElo(Integer minOpponentElo) {
    this.minOpponentElo = minOpponentElo;
  }

  public Integer getMaxOpponentElo() {
    return maxOpponentElo;
  }

  public void setMaxOpponentElo(Integer maxOpponentElo) {
    this.maxOpponentElo = maxOpponentElo;
  }

  public Integer getMaxTravelDistanceMeters() {
    return maxTravelDistanceMeters;
  }

  public void setMaxTravelDistanceMeters(Integer maxTravelDistanceMeters) {
    this.maxTravelDistanceMeters = maxTravelDistanceMeters;
  }

  public Boolean getOpenToMatchmaking() {
    return openToMatchmaking;
  }

  public void setOpenToMatchmaking(Boolean openToMatchmaking) {
    this.openToMatchmaking = openToMatchmaking;
  }

  public Boolean getIsPrimarySport() {
    return isPrimarySport;
  }

  public void setIsPrimarySport(Boolean isPrimarySport) {
    this.isPrimarySport = isPrimarySport;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
