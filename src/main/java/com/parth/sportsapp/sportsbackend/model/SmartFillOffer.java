package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One Smart Fill notification sent to one player for one court slot.
 *
 * <p>Three jobs:</p>
 * <ul>
 *   <li><b>Dedup</b> — the unique constraint stops the same player being
 *       notified twice for the same court + slot.</li>
 *   <li><b>Cooldown</b> — counting distinct slots offered per court per day
 *       caps how often a vendor can blast nearby players.</li>
 *   <li><b>Conversion analytics (future)</b> — joining offers to bookings
 *       answers "17 notified → how many actually booked?"</li>
 * </ul>
 */
@Entity
@Table(name = "smart_fill_offers",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_offer_court_slot_user",
        columnNames = {"court_id", "slot_start", "user_id"}),
    indexes = {
        @Index(name = "idx_offer_court_sent", columnList = "court_id, sent_at"),
        @Index(name = "idx_offer_user", columnList = "user_id")
    })
public class SmartFillOffer {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "court_id", nullable = false)
  private UUID courtId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "slot_start", nullable = false)
  private LocalDateTime slotStart;

  @Column(name = "slot_end", nullable = false)
  private LocalDateTime slotEnd;

  @Column(name = "sent_at", nullable = false)
  private LocalDateTime sentAt = LocalDateTime.now();

  public SmartFillOffer() {}

  public UUID getId() { return id; }
  public void setId(UUID v) { this.id = v; }

  public UUID getCourtId() { return courtId; }
  public void setCourtId(UUID v) { this.courtId = v; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID v) { this.userId = v; }

  public LocalDateTime getSlotStart() { return slotStart; }
  public void setSlotStart(LocalDateTime v) { this.slotStart = v; }

  public LocalDateTime getSlotEnd() { return slotEnd; }
  public void setSlotEnd(LocalDateTime v) { this.slotEnd = v; }

  public LocalDateTime getSentAt() { return sentAt; }
  public void setSentAt(LocalDateTime v) { this.sentAt = v; }
}
