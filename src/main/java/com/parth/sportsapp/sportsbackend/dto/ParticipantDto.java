package com.parth.sportsapp.sportsbackend.dto;

public class ParticipantDto {
  private UserSummaryDto user; // Re-use your existing summary (ID, Name, Pic)
  private boolean isHost;      // Did they organize it?
  private String status;       // PENDING, ACCEPTED, REJECTED


  private String teamName; 


  

public String getTeamName() { return teamName; }
public void setTeamName(String teamName) { this.teamName = teamName; }
  // Getters, Setters, Constructor

  public UserSummaryDto getUser() {
    return user;
  }

  public void setUser(UserSummaryDto user) {
    this.user = user;
  }

  public boolean isHost() {
    return isHost;
  }

  public void setHost(boolean host) {
    isHost = host;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
