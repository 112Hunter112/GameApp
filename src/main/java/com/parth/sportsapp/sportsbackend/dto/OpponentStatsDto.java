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
}
