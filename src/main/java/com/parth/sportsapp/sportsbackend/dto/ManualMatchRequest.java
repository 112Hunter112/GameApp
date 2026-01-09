package com.parth.sportsapp.sportsbackend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ManualMatchRequest {
  private UUID opponentId;       // Optional (For existing users)
  private String opponentEmail;  // Optional (For external users)
  private String opponentName;   // Optional (For external users)

  private LocalDateTime date;
  private String score;
  private String notes;

  // Getters and Setters


  public UUID getOpponentId() {
    return opponentId;
  }

  public void setOpponentId(UUID opponentId) {
    this.opponentId = opponentId;
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

  public String getOpponentEmail() {
    return opponentEmail;
  }

  public void setOpponentEmail(String opponentEmail) {
    this.opponentEmail = opponentEmail;
  }

  public String getOpponentName() {
    return opponentName;
  }

  public void setOpponentName(String opponentName) {
    this.opponentName = opponentName;
  }
}
