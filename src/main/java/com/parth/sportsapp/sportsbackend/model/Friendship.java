package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"requester_id", "receiver_id"}) // makes both these
        // variables me unique
    }
)
public class Friendship {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  //many requests can be sent by one user, but in Friendship each row is only for 1 friendship
  @ManyToOne
  @JoinColumn(name = "requester_id", nullable = false)
  private User requester;

  @ManyToOne
  @JoinColumn(name = "receiver_id", nullable = false)
  private User receiver;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FriendshipStatus status;

  @Column(nullable = false, updatable = false)
  @org.hibernate.annotations.CreationTimestamp
  private LocalDateTime createdAt;

  public Friendship() {} // JPA needs this

  public Friendship(User requester, User receiver, FriendshipStatus status) {
    this.requester = requester;
    this.receiver = receiver;
    this.status = status;
    this.createdAt = LocalDateTime.now(); // Auto-set time
  }

  @Column(length = 500)
  private String message;
  // --- GETTERS & SETTERS ---

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public User getRequester() {
    return requester;
  }

  public void setRequester(User requester) {
    this.requester = requester;
  }

  public User getReceiver() {
    return receiver;
  }

  public void setReceiver(User receiver) {
    this.receiver = receiver;
  }

  public FriendshipStatus getStatus() {
    return status;
  }

  public void setStatus(FriendshipStatus status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
