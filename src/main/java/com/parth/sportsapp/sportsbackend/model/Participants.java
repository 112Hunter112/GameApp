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

  private String status; // e.g., REQUESTED, ACCEPTED, REJECTED

  @Column(name = "is_host")
  private boolean isHost = false;

  public Participants() {}

  // --- Getters and Setters ---
  public ParticipantsId getId() { return id; }
  public void setId(ParticipantsId id) { this.id = id; }

  public Match getMatch() { return match; }
  public void setMatch(Match match) { this.match = match; }

  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public boolean isHost() { return isHost; }
  public void setHost(boolean host) { isHost = host; }
}
