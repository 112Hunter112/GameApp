package com.parth.sportsapp.sportsbackend.dto;


public class MonthlyStatsDto {
  private long totalMatches;
  private long wins;
  private long losses;
  private double winRate;
  private String month;

  public MonthlyStatsDto(long totalMatches, long wins, long losses,
      double winRate, String month) {
    this.totalMatches = totalMatches;
    this.wins = wins;
    this.losses = losses;
    this.winRate = winRate;
    this.month = month;
  }

  // Getters and setters
  // Add these at the bottom of the class
  public long getTotalMatches() { return totalMatches; }
  public long getWins() { return wins; }
  public long getLosses() { return losses; }
  public double getWinRate() { return winRate; }
  public String getMonth() { return month; }
}
