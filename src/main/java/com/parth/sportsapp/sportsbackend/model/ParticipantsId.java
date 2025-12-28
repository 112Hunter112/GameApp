package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ParticipantsId implements Serializable {

  private UUID matchId;
  private UUID userId;

  public ParticipantsId() {}

  public ParticipantsId(UUID matchId, UUID userId) {
    this.matchId = matchId;
    this.userId = userId;
  }

  // --- Getters and Setters ---
  public UUID getMatchId() { return matchId; }
  public void setMatchId(UUID matchId) { this.matchId = matchId; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ParticipantsId that = (ParticipantsId) o;
    return Objects.equals(matchId, that.matchId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(matchId, userId);
  }
}
