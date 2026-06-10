package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.BookingStatus;
import com.parth.sportsapp.sportsbackend.model.CancellationActor;
import com.parth.sportsapp.sportsbackend.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Everything either side needs to render a booking card:
 * the player sees venue/court details; the owner additionally sees who booked.
 */
public class BookingResponse {

  private UUID id;
  private BookingStatus status;

  // When
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private int durationMinutes;

  // Where
  private UUID courtId;
  private String courtNumber;
  private String sportName;
  private UUID venueId;
  private String venueName;
  private String venueAddress;

  // Who (populated for the venue owner's dashboard; null in player views)
  private UUID playerId;
  private String playerName;

  // Money
  private BigDecimal totalPrice;
  private PaymentStatus paymentStatus;

  // Misc
  private String notes;
  private LocalDateTime createdAt;
  private LocalDateTime confirmedAt;
  private LocalDateTime cancelledAt;
  private CancellationActor cancelledBy;
  private String cancellationReason;

  /** Whether the requesting user may still cancel this booking (server-computed). */
  private boolean canCancel;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public BookingStatus getStatus() { return status; }
  public void setStatus(BookingStatus status) { this.status = status; }

  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

  public int getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

  public UUID getCourtId() { return courtId; }
  public void setCourtId(UUID courtId) { this.courtId = courtId; }

  public String getCourtNumber() { return courtNumber; }
  public void setCourtNumber(String courtNumber) { this.courtNumber = courtNumber; }

  public String getSportName() { return sportName; }
  public void setSportName(String sportName) { this.sportName = sportName; }

  public UUID getVenueId() { return venueId; }
  public void setVenueId(UUID venueId) { this.venueId = venueId; }

  public String getVenueName() { return venueName; }
  public void setVenueName(String venueName) { this.venueName = venueName; }

  public String getVenueAddress() { return venueAddress; }
  public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }

  public UUID getPlayerId() { return playerId; }
  public void setPlayerId(UUID playerId) { this.playerId = playerId; }

  public String getPlayerName() { return playerName; }
  public void setPlayerName(String playerName) { this.playerName = playerName; }

  public BigDecimal getTotalPrice() { return totalPrice; }
  public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

  public PaymentStatus getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

  public LocalDateTime getCancelledAt() { return cancelledAt; }
  public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

  public CancellationActor getCancelledBy() { return cancelledBy; }
  public void setCancelledBy(CancellationActor cancelledBy) { this.cancelledBy = cancelledBy; }

  public String getCancellationReason() { return cancellationReason; }
  public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

  public boolean isCanCancel() { return canCancel; }
  public void setCanCancel(boolean canCancel) { this.canCancel = canCancel; }
}
