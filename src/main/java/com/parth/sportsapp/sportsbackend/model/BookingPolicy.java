package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Per-venue booking rules. One row per venue; venues without a row get
 * {@link #defaults(Venue)} so booking works out of the box.
 *
 * The owner controls "how their hour system works":
 *  - slotIncrementMinutes: the time grid (30 = half-hour starts, 60 = on the hour)
 *  - min/maxBookingMinutes: shortest and longest allowed session (1.5h, 2.5h, ...
 *    are possible whenever the increment is 30)
 *  - autoConfirm: instant book (Airbnb "Instant Book") vs request-to-book
 *  - openingHours: "MONDAY" -> "08:00-22:00"; a missing day means closed
 */
@Entity
@Table(name = "booking_policies")
public class BookingPolicy {

  public static final int DEFAULT_INCREMENT = 60;
  public static final int DEFAULT_MIN_MINUTES = 60;
  public static final int DEFAULT_MAX_MINUTES = 180;
  public static final int DEFAULT_ADVANCE_DAYS = 14;
  public static final int DEFAULT_MIN_NOTICE_MINUTES = 60;
  public static final int DEFAULT_CANCEL_CUTOFF_MINUTES = 120;
  public static final String DEFAULT_DAY_HOURS = "08:00-22:00";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne
  @JoinColumn(name = "venue_id", nullable = false, unique = true)
  private Venue venue;

  /** Time grid for slot starts AND durations. Allowed: 30 or 60. */
  @Column(name = "slot_increment_minutes", nullable = false)
  private int slotIncrementMinutes = DEFAULT_INCREMENT;

  @Column(name = "min_booking_minutes", nullable = false)
  private int minBookingMinutes = DEFAULT_MIN_MINUTES;

  @Column(name = "max_booking_minutes", nullable = false)
  private int maxBookingMinutes = DEFAULT_MAX_MINUTES;

  /** How far into the future players may book. */
  @Column(name = "advance_booking_days", nullable = false)
  private int advanceBookingDays = DEFAULT_ADVANCE_DAYS;

  /** Bookings must start at least this many minutes from now. */
  @Column(name = "min_notice_minutes", nullable = false)
  private int minNoticeMinutes = DEFAULT_MIN_NOTICE_MINUTES;

  /** Players may cancel until this many minutes before start. */
  @Column(name = "cancellation_cutoff_minutes", nullable = false)
  private int cancellationCutoffMinutes = DEFAULT_CANCEL_CUTOFF_MINUTES;

  /** true = instant book; false = owner must confirm each request. */
  @Column(name = "auto_confirm", nullable = false)
  private boolean autoConfirm = true;

  /**
   * Weekly opening hours, e.g. {"MONDAY": "08:00-22:00", ...}.
   * Keys are java.time.DayOfWeek names; a missing key = closed that day.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "opening_hours", columnDefinition = "jsonb")
  private Map<String, String> openingHours;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public BookingPolicy() {}

  /** In-memory defaults for venues whose owner hasn't configured anything yet. Not persisted. */
  public static BookingPolicy defaults(Venue venue) {
    BookingPolicy p = new BookingPolicy();
    p.setVenue(venue);
    p.setOpeningHours(Map.of(
        "MONDAY", DEFAULT_DAY_HOURS,
        "TUESDAY", DEFAULT_DAY_HOURS,
        "WEDNESDAY", DEFAULT_DAY_HOURS,
        "THURSDAY", DEFAULT_DAY_HOURS,
        "FRIDAY", DEFAULT_DAY_HOURS,
        "SATURDAY", DEFAULT_DAY_HOURS,
        "SUNDAY", DEFAULT_DAY_HOURS));
    return p;
  }

  // --- Getters and Setters ---

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Venue getVenue() { return venue; }
  public void setVenue(Venue venue) { this.venue = venue; }

  public int getSlotIncrementMinutes() { return slotIncrementMinutes; }
  public void setSlotIncrementMinutes(int slotIncrementMinutes) { this.slotIncrementMinutes = slotIncrementMinutes; }

  public int getMinBookingMinutes() { return minBookingMinutes; }
  public void setMinBookingMinutes(int minBookingMinutes) { this.minBookingMinutes = minBookingMinutes; }

  public int getMaxBookingMinutes() { return maxBookingMinutes; }
  public void setMaxBookingMinutes(int maxBookingMinutes) { this.maxBookingMinutes = maxBookingMinutes; }

  public int getAdvanceBookingDays() { return advanceBookingDays; }
  public void setAdvanceBookingDays(int advanceBookingDays) { this.advanceBookingDays = advanceBookingDays; }

  public int getMinNoticeMinutes() { return minNoticeMinutes; }
  public void setMinNoticeMinutes(int minNoticeMinutes) { this.minNoticeMinutes = minNoticeMinutes; }

  public int getCancellationCutoffMinutes() { return cancellationCutoffMinutes; }
  public void setCancellationCutoffMinutes(int cancellationCutoffMinutes) { this.cancellationCutoffMinutes = cancellationCutoffMinutes; }

  public boolean isAutoConfirm() { return autoConfirm; }
  public void setAutoConfirm(boolean autoConfirm) { this.autoConfirm = autoConfirm; }

  public Map<String, String> getOpeningHours() { return openingHours; }
  public void setOpeningHours(Map<String, String> openingHours) { this.openingHours = openingHours; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
