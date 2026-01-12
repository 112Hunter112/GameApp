package com.parth.sportsapp.sportsbackend.dto;

import java.util.UUID;

public class OpponentStatsDto {
  private UUID opponentId;
  private String opponentName;
  private long totalMatches;
  private long wins;
  private long losses;

  public OpponentStatsDto(UUID opponentId, String opponentName,
      long totalMatches, long wins, long losses) {
    this.opponentId = opponentId;
    this.opponentName = opponentName;
    this.totalMatches = totalMatches;
    this.wins = wins;
    this.losses = losses;
  }

  // Getters and setters

  public UUID getOpponentId() {
    return opponentId;
  }

  public void setOpponentId(UUID opponentId) {
    this.opponentId = opponentId;
  }

  public String getOpponentName() {
    return opponentName;
  }

  public void setOpponentName(String opponentName) {
    this.opponentName = opponentName;
  }

  public long getTotalMatches() {
    return totalMatches;
  }

  public void setTotalMatches(long totalMatches) {
    this.totalMatches = totalMatches;
  }

  public long getWins() {
    return wins;
  }

  public void setWins(long wins) {
    this.wins = wins;
  }

  public long getLosses() {
    return losses;
  }

  public void setLosses(long losses) {
    this.losses = losses;
  }
}
