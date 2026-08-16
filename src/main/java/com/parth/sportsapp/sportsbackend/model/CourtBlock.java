package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An owner-created hold on a court: maintenance, a private event, a league night.
 * Blocked time shows as unavailable in the availability grid and rejects bookings,
 * exactly like a confirmed booking would.
 */
@Entity
@Table(name = "court_blocks", indexes = {
    // countOverlapping runs inside the booking transaction while the court row
    // is locked — a scan here directly stretches lock hold time on busy courts.
    @Index(name = "idx_court_block_court_time", columnList = "court_id, start_time, end_time")
})
public class CourtBlock {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "court_id", nullable = false)
  private Courts court;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  /** Shown to the owner in their calendar ("Resurfacing", "League night"). */
  @Column(length = 200)
  private String reason;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  public CourtBlock() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Courts getCourt() { return court; }
  public void setCourt(Courts court) { this.court = court; }

  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
