package com.parth.sportsapp.sportsbackend.dto;

public class HeadToHeadDto {
  private long totalMatches;
  private long myWins;
  private long opponentWins;
  private MatchResponse mostRecentMatch;
  private String opponentName;

  public HeadToHeadDto(long totalMatches, long myWins, long opponentWins,
      MatchResponse mostRecentMatch, String opponentName) {
    this.totalMatches = totalMatches;
    this.myWins = myWins;
    this.opponentWins = opponentWins;
    this.mostRecentMatch = mostRecentMatch;
    this.opponentName = opponentName;
  }

  // Getters and setters
}
