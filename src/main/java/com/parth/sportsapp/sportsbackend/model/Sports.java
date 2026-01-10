package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

@Entity

@Table(name = "sports")
public class Sports {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "sport_name", nullable = false, unique = true)
  @NotBlank(message = "Sport name is required")
  private String sportName;

  @Column(name = "min_player")
  private int minPlayers;

  @Column(name = "max_player")
  private int maxPlayers;

  @Column(name = "icon_url")
  private String iconURL;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "is_active")
  private boolean isActive = true;

  @OneToMany(mappedBy = "sports")
  private List<UserPreference> preferences;

  @OneToMany(mappedBy = "sports")
  private List<Courts> courts;

  @Enumerated(EnumType.STRING) // Stores "SETS" or "POINTS" in the database
  @Column(name = "scoring_type", nullable = false)
  private ScoringType scoringType;




  // -----------SETTERS and GETTERS
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getSportName() {
    return sportName;
  }

  public void setSportName(String sportName) {
    this.sportName = sportName;
  }

  public int getMinPlayers() {
    return minPlayers;
  }

  public void setMinPlayers(int minPlayers) {
    this.minPlayers = minPlayers;
  }

  public int getMaxPlayers() {
    return maxPlayers;
  }

  public void setMaxPlayers(int maxPlayers) {
    this.maxPlayers = maxPlayers;
  }

  public String getIconURL() {
    return iconURL;
  }

  public void setIconURL(String iconURL) {
    this.iconURL = iconURL;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public List<UserPreference> getPreferences() {
    return preferences;
  }

  public void setPreferences(List<UserPreference> preferences) {
    this.preferences = preferences;
  }

  public List<Courts> getCourts() {
    return courts;
  }

  public void setCourts(List<Courts> courts) {
    this.courts = courts;
  }

  public ScoringType getScoringType() {
    return scoringType;
  }

  public void setScoringType(ScoringType scoringType) {
    this.scoringType = scoringType;
  }
}
