package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.MatchSource;
import com.parth.sportsapp.sportsbackend.model.MatchVerificationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MatchResponse {

  // --- 1. Identity ---
  private UUID id;             // Frontend needs this to open "Match Details"
  private UserSummaryDto createdBy; // "Logged by Parth"

  // --- 2. The Game Data (Your List) ---
  private String score;        // "6-4, 6-4"
  private LocalDateTime date;  // "Oct 12, 5:00 PM"
  private UserSummaryDto winner; // Show a trophy icon next to this user

  // --- 3. Verification ---
  private MatchSource source;  // APP_BOOKING vs MANUAL
  private MatchVerificationStatus verificationStatus; // Shows "Pending" badge

  // --- 4. Context ---
  private String notes;        // "Played at Central Park"
  private UUID linkedBookingId; // If valid, show "View Booking" button

  // --- 5. The Players ---
  private List<ParticipantDto> participants;

  // --- 6. External Opponent (Fallback) ---
  // If participants list is empty (external user), show this name instead
  private String externalOpponentName;

  private String winningTeam;





  // Getters and Setters


  public String getWinningTeam() { return winningTeam; }
  public void setWinningTeam(String winningTeam) { this.winningTeam = winningTeam; }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UserSummaryDto getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UserSummaryDto createdBy) {
    this.createdBy = createdBy;
  }

  public String getScore() {
    return score;
  }

  public void setScore(String score) {
    this.score = score;
  }

  public LocalDateTime getDate() {
    return date;
  }

  public void setDate(LocalDateTime date) {
    this.date = date;
  }

  public UserSummaryDto getWinner() {
    return winner;
  }

  public void setWinner(UserSummaryDto winner) {
    this.winner = winner;
  }

  public MatchSource getSource() {
    return source;
  }

  public void setSource(MatchSource source) {
    this.source = source;
  }

  public MatchVerificationStatus getVerificationStatus() {
    return verificationStatus;
  }

  public void setVerificationStatus(MatchVerificationStatus verificationStatus) {
    this.verificationStatus = verificationStatus;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public UUID getLinkedBookingId() {
    return linkedBookingId;
  }

  public void setLinkedBookingId(UUID linkedBookingId) {
    this.linkedBookingId = linkedBookingId;
  }

  public List<ParticipantDto> getParticipants() {
    return participants;
  }

  public void setParticipants(List<ParticipantDto> participants) {
    this.participants = participants;
  }

  public String getExternalOpponentName() {
    return externalOpponentName;
  }

  public void setExternalOpponentName(String externalOpponentName) {
    this.externalOpponentName = externalOpponentName;
  }
}
