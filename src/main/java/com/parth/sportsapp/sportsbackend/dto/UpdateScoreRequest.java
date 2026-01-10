package com.parth.sportsapp.sportsbackend.dto;

public class UpdateScoreRequest {
  private String score;
  private String winningTeam; // "TEAM_A", "TEAM_B", or "DRAW"

  // Getters and Setters
  public String getScore() { return score; }
  public void setScore(String score) { this.score = score; }
  public String getWinningTeam() { return winningTeam; }
  public void setWinningTeam(String winningTeam) { this.winningTeam = winningTeam; }
}
