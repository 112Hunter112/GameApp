package com.parth.sportsapp.sportsbackend.dto;

public class UserStatsDto {
  private long totalMatches;
  private long wins;
  private long losses;
  private double winRate;

  public UserStatsDto(long totalMatches, long wins, long losses, double winRate) {
    this.totalMatches = totalMatches;
    this.wins = wins;
    this.losses = losses;
    this.winRate = winRate;
  }

  // Getters and Setters
  public long getTotalMatches() { return totalMatches; }
  public void setTotalMatches(long totalMatches) { this.totalMatches = totalMatches; }
  public long getWins() { return wins; }
  public void setWins(long wins) { this.wins = wins; }
  public long getLosses() { return losses; }
  public void setLosses(long losses) { this.losses = losses; }
  public double getWinRate() { return winRate; }
  public void setWinRate(double winRate) { this.winRate = winRate; }
}
