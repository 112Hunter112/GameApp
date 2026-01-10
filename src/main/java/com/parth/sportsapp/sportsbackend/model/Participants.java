package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "participants")
public class Participants {

  @EmbeddedId
  private ParticipantsId id;

  @ManyToOne
  @MapsId("matchId")
  @JoinColumn(name = "match_id")
  private Match match;

  @ManyToOne
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  private ParticipationStatus status;// e.g., REQUESTED, ACCEPTED, REJECTED

  @Column(name = "is_host")
  private boolean isHost = false;

  @Column(name = "team_name")
  private String teamName; // "HOME", "AWAY" or "TEAM_A", "TEAM_B"

  public Participants() {}

  // --- Getters and Setters ---
  public ParticipantsId getId() { return id; }
  public void setId(ParticipantsId id) { this.id = id; }

  public Match getMatch() { return match; }
  public void setMatch(Match match) { this.match = match; }

  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }

  public ParticipationStatus getStatus() {
    return status;
  }

  public void setStatus(ParticipationStatus status) {
    this.status = status;
  }

  public boolean isHost() { return isHost; }
  public void setHost(boolean host) { isHost = host; }

  public String getTeamName() {
    return teamName;
  }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }
}
