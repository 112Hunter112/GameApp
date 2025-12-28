package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_results")
public class MatchResults {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne
  @JoinColumn(name = "match_id", nullable = false, unique = true)
  private Match match;

  @Column(name = "team_a_score")
  private Integer teamAScore;

  @Column(name = "team_b_score")
  private Integer teamBScore;

  @Column(name = "winner_team")
  private String winnerTeam; // e.g., "A", "B", or "DRAW"

  @CreationTimestamp
  @Column(name = "completed_at", updatable = false)
  private LocalDateTime completedAt;

  public MatchResults() {}

  // --- Getters and Setters ---
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Match getMatch() { return match; }
  public void setMatch(Match match) { this.match = match; }

  public Integer getTeamAScore() { return teamAScore; }
  public void setTeamAScore(Integer teamAScore) { this.teamAScore = teamAScore; }

  public Integer getTeamBScore() { return teamBScore; }
  public void setTeamBScore(Integer teamBScore) { this.teamBScore = teamBScore; }

  public String getWinnerTeam() { return winnerTeam; }
  public void setWinnerTeam(String winnerTeam) { this.winnerTeam = winnerTeam; }

  public LocalDateTime getCompletedAt() { return completedAt; }
  public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
