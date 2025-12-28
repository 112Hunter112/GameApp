package com.parth.sportsapp.sportsbackend.model;


import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class UserPreference {

  @EmbeddedId
  private UserPreferenceId id;

  @ManyToOne
  @MapsId("userId") // Links the 'userId' in our Composite Key to the User entity
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne
  @MapsId("sportId") // Links the 'sportId' in our Composite Key to the Sport entity
  @JoinColumn(name = "sport_id")
  private Sports sports;

  @Column(name = "proficiency_level")
  private String proficiencyLevel;

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
}
