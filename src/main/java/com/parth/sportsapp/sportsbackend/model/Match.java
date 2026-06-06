package com.parth.sportsapp.sportsbackend.model;

import com.parth.sportsapp.sportsbackend.model.MatchSource;
import com.parth.sportsapp.sportsbackend.model.MatchVerificationStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "matches")
public class Match {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;


  @OneToOne
  @JoinColumn(name = "booking_id", nullable = true)
  private Booking booking; // was this a booking via the app or not, thus it can be NULL

  @ManyToOne
  @JoinColumn(name = "created_by_user_id", nullable = false)
  private User createdByUser; // who created this match

  @Enumerated(EnumType.STRING)
  @Column(name = "source")
  private MatchSource source = MatchSource.APP_BOOKING; // how was this match history entered?

  @Enumerated(EnumType.STRING)
  @Column(name = "verification_status")
  private MatchVerificationStatus verificationStatus = MatchVerificationStatus.CONFIRMED;
  // Default to CONFIRMED for App Bookings, change to PENDING for Manual

  // --------------------------------------------------------
  // 3. NEW: Flexible Score (Strings allow "6-4, 6-4" or "2-1")
  // --------------------------------------------------------
  @Column(name = "score_summary")
  private String score; // e.g., "6-4, 6-3"


  @Column(name = "match_date", nullable = false)
  private LocalDateTime matchDate;

  // --- EXISTING FIELDS (Keep these for your "Lobby/LFG" features) ---

  @Column(name = "is_private")
  private boolean isPrivate = false;

  @Column(name = "status", nullable = false)
  private String status = "OPEN"; // OPEN/FULL (Lobby Status)

  /**
   * Set to true the first (and only) time Elo ratings are applied for this
   * match. Prevents re-confirmation from doubling the rating change.
   */
  @Column(name = "ratings_applied", nullable = false)
  private boolean ratingsApplied = false;

  // This is the list of all participants for that match
  @OneToMany(mappedBy = "match", cascade = CascadeType.ALL) // Cascade allows saving participants with match
  private List<Participants> participants;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(columnDefinition = "TEXT")
  private String description;

  // --- EXTERNAL OPPONENT FIELDS ---
  // Used ONLY if the opponent is not on the app

  @Column(name = "external_opponent_email")
  private String externalOpponentEmail;

  @Column(name = "external_opponent_name")
  private String externalOpponentName;

  @Column(name = "external_verification_token")
  private String externalVerificationToken; // The magic code in the email link

  @Column(name = "winning_team")
  private String winningTeam; // "TEAM_A" or "TEAM_B" (or "DRAW")

  @ManyToOne
  @JoinColumn(name = "sport_id", nullable = false)
  private Sports sport; // e.g., "TENNIS", "SOCCER", "PADEL"

  // --- CONSTRUCTORS, GETTERS & SETTERS ---

  // Update Getters/Setters
  public String getWinningTeam() { return winningTeam; }
  public void setWinningTeam(String winningTeam) { this.winningTeam = winningTeam; }

  public Match() {}

  // Getters/Setters...
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Booking getBooking() { return booking; }
  public void setBooking(Booking booking) { this.booking = booking; }

  public MatchSource getSource() { return source; }
  public void setSource(MatchSource source) { this.source = source; }

  public MatchVerificationStatus getVerificationStatus() { return verificationStatus; }
  public void setVerificationStatus(MatchVerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

  public String getScore() { return score; }
  public void setScore(String score) { this.score = score; }


  public User getCreatedByUser() { return createdByUser; }
  public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }

  public LocalDateTime getMatchDate() { return matchDate; }
  public void setMatchDate(LocalDateTime matchDate) { this.matchDate = matchDate; }

  public List<Participants> getParticipants() { return participants; }
  public void setParticipants(List<Participants> participants) { this.participants = participants; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public boolean isRatingsApplied() { return ratingsApplied; }
  public void setRatingsApplied(boolean v) { this.ratingsApplied = v; }

  public boolean isPrivate() {
    return isPrivate;
  }

  public void setPrivate(boolean aPrivate) {
    isPrivate = aPrivate;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getExternalOpponentEmail() {
    return externalOpponentEmail;
  }

  public void setExternalOpponentEmail(String externalOpponentEmail) {
    this.externalOpponentEmail = externalOpponentEmail;
  }

  public String getExternalOpponentName() {
    return externalOpponentName;
  }

  public void setExternalOpponentName(String externalOpponentName) {
    this.externalOpponentName = externalOpponentName;
  }

  public String getExternalVerificationToken() {
    return externalVerificationToken;
  }

  public void setExternalVerificationToken(String externalVerificationToken) {
    this.externalVerificationToken = externalVerificationToken;
  }

  public Sports getSport() {
    return sport;
  }

  public void setSport(Sports sport) {
    this.sport = sport;
  }
}
