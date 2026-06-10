package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // Many bookings can be made by one user
  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // Many bookings can be made for one court
  @ManyToOne
  @JoinColumn(name = "court_id", nullable = false)
  private Courts court;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BookingStatus status = BookingStatus.PENDING;

  @Column(name = "total_price", precision = 10, scale = 2)
  private BigDecimal totalPrice;

  @Column(name = "payment_id")
  private String paymentId; // Stripe Transaction ID

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status")
  private PaymentStatus paymentStatus = PaymentStatus.PENDING; // PAID, REFUNDED

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method")
  private PaymentMethod paymentMethod = PaymentMethod.CASH; // pay-at-venue until Stripe lands

  /** Optional note from the player to the venue ("we need 4 rackets", etc.). */
  @Column(columnDefinition = "TEXT")
  private String notes;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "cancelled_by")
  private CancellationActor cancelledBy;

  /** Reason supplied on owner decline / either party's cancel. Shown to the other side. */
  @Column(name = "cancellation_reason", columnDefinition = "TEXT")
  private String cancellationReason;

  /**
   * Bidirectional One-to-One relationship with Match.
   * mappedBy points to the 'booking' field inside the Match entity.
   */
  @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
  private Match match;

  // --- Constructors ---

  public Booking() {}

  // --- Getters and Setters ---

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }

  public Courts getCourt() { return court; }
  public void setCourt(Courts court) { this.court = court; }

  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

  public BookingStatus getStatus() { return status; }
  public void setStatus(BookingStatus status) { this.status = status; }

  public BigDecimal getTotalPrice() { return totalPrice; }
  public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

  public String getPaymentId() { return paymentId; }
  public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

  public PaymentStatus getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

  public PaymentMethod getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getCancelledAt() { return cancelledAt; }
  public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }

  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

  public CancellationActor getCancelledBy() { return cancelledBy; }
  public void setCancelledBy(CancellationActor cancelledBy) { this.cancelledBy = cancelledBy; }

  public String getCancellationReason() { return cancellationReason; }
  public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

  public Match getMatch() {
    return match;
  }

  public void setMatch(Match match) {
    this.match = match;
  }
}
