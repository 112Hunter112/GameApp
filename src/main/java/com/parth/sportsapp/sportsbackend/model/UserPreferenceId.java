package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserPreferenceId implements Serializable {

  private UUID userId;
  private UUID sportId;

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getSportId() {
    return sportId;
  }

  public void setSportId(UUID sportId) {
    this.sportId = sportId;
  }

  public UserPreferenceId() {}

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserPreferenceId userPreferenceId = (UserPreferenceId) o;
    return userId.equals(userPreferenceId.userId) && sportId.equals(userPreferenceId.sportId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, sportId);
  }
}
