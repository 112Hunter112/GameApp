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

  public long getTotalMatches() {
    return totalMatches;
  }

  public void setTotalMatches(long totalMatches) {
    this.totalMatches = totalMatches;
  }

  public long getMyWins() {
    return myWins;
  }

  public void setMyWins(long myWins) {
    this.myWins = myWins;
  }

  public long getOpponentWins() {
    return opponentWins;
  }

  public void setOpponentWins(long opponentWins) {
    this.opponentWins = opponentWins;
  }

  public MatchResponse getMostRecentMatch() {
    return mostRecentMatch;
  }

  public void setMostRecentMatch(MatchResponse mostRecentMatch) {
    this.mostRecentMatch = mostRecentMatch;
  }

  public String getOpponentName() {
    return opponentName;
  }

  public void setOpponentName(String opponentName) {
    this.opponentName = opponentName;
  }
}
