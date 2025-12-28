package com.parth.sportsapp.sportsbackend.model;

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

  /**
   * The physical reservation this match is tied to.
   * This is the 'Owning' side of the One-to-One relationship.
   */
  @OneToOne
  @JoinColumn(name = "booking_id", nullable = false, unique = true)
  private Booking booking;

  /**
   * The user who created the match (The Host).
   */
  @ManyToOne
  @JoinColumn(name = "created_by_user_id", nullable = false)
  private User createdByUser;

  @Column(name = "match_date", nullable = false)
  private LocalDateTime matchDate;

  @Column(name = "is_private")
  private boolean isPrivate = false;

  @Column(name = "skill_requirement")
  private String skillRequirement; // e.g., BEGINNER, INTERMEDIATE, PRO

  @Column(name = "current_players")
  private Integer currentPlayers = 1; // Starts with the host

  @Column(name = "max_players")
  private Integer maxPlayers;

  // Status: OPEN, FULL, COMPLETED, CANCELLED
  @Column(nullable = false)
  private String status = "OPEN";

  @Column(name = "chat_room_id")
  private String chatRoomId; // Connection to your Go chat service

  @Column(columnDefinition = "TEXT")
  private String description;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "match")
  private List<Participants> participants;

  @OneToOne(mappedBy = "match", cascade = CascadeType.ALL)
  private MatchResults result;

  // --- Constructors ---
  public Match() {}

  // --- Getters and Setters ---
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Booking getBooking() { return booking; }
  public void setBooking(Booking booking) { this.booking = booking; }

  public User getCreatedByUser() { return createdByUser; }
  public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }

  public LocalDateTime getMatchDate() { return matchDate; }
  public void setMatchDate(LocalDateTime matchDate) { this.matchDate = matchDate; }

  public boolean isPrivate() { return isPrivate; }
  public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

  public String getSkillRequirement() { return skillRequirement; }
  public void setSkillRequirement(String skillRequirement) { this.skillRequirement = skillRequirement; }

  public Integer getCurrentPlayers() { return currentPlayers; }
  public void setCurrentPlayers(Integer currentPlayers) { this.currentPlayers = currentPlayers; }

  public Integer getMaxPlayers() { return maxPlayers; }
  public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public String getChatRoomId() { return chatRoomId; }
  public void setChatRoomId(String chatRoomId) { this.chatRoomId = chatRoomId; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public List<Participants> getParticipants() {
    return participants;
  }

  public void setParticipants(List<Participants> participants) {
    this.participants = participants;
  }

  public MatchResults getResult() {
    return result;
  }

  public void setResult(MatchResults result) {
    this.result = result;
  }
}
