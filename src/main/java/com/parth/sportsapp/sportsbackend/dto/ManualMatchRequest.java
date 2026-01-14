package com.parth.sportsapp.sportsbackend.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List; // Import List
import java.util.UUID;

public class ManualMatchRequest {

  // --- 1. TEAMS (Now Lists instead of Single Values) ---
  private List<UUID> opponentIds;       // List of opponents on the App
  private List<String> opponentEmails;  // List of external opponents (Email)
  // Note: opponentName is tricky with lists. Usually, frontend handles name display until they join.
  // If you need names for email invites, you might need a small inner class or Map,
  // but for now, we can just use the email as the name or add a List<String> opponentNames.

  private List<UUID> teammateIds;       // Friends on YOUR team (e.g., Doubles partner)



  // --- 2. GAME DETAILS ---
  private String sport;                 // "TENNIS_SINGLES", "SOCCER_5V5", etc.
  private LocalDateTime date;
  private String score;                 // "6-4, 6-4"
  private String notes;                 // "Played at Central Park"




  private String winningTeam; // Expected values: "TEAM_A", "TEAM_B"


  public String getWinningTeam() { return winningTeam; }
  public void setWinningTeam(String winningTeam) { this.winningTeam = winningTeam; }




  // --- GETTERS AND SETTERS ---

  public List<UUID> getOpponentIds() {
    if (opponentIds == null) {
      return new ArrayList<>();
    }
    return opponentIds;
  }

  public void setOpponentIds(List<UUID> opponentIds) {
    this.opponentIds = opponentIds;
  }

  public List<String> getOpponentEmails() {
    if (opponentEmails == null) {
      return new ArrayList<>();
    }
    return opponentEmails;
  }

  public void setOpponentEmails(List<String> opponentEmails) {
    this.opponentEmails = opponentEmails;
  }

  public List<UUID> getTeammateIds() {
    // If null, return an empty list so loops don't crash
    if (teammateIds == null) {
      return new ArrayList<>();
    }
    return teammateIds;
  }

  public void setTeammateIds(List<UUID> teammateIds) {
    this.teammateIds = teammateIds;
  }

  public String getSport() {
    return sport;
  }

  public void setSport(String sport) {
    this.sport = sport;
  }

  public LocalDateTime getDate() {
    return date;
  }

  public void setDate(LocalDateTime date) {
    this.date = date;
  }

  public String getScore() {
    return score;
  }

  public void setScore(String score) {
    this.score = score;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  // Helper for your Service logic to avoid null checks everywhere
  public String getOpponentName() {
    // You can remove this or keep it as a fallback if you only expect 1 external user most times
    return "Invited Player";
  }
}
