package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.BookingStatus;
import com.parth.sportsapp.sportsbackend.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One booking row in the vendor's schedule view. Player identity is trimmed
 * to "First L." — the vendor needs to know who to expect at the desk, not
 * the player's full profile.
 */
public class VendorBookingEntry {

  private UUID bookingId;
  private String courtNumber;
  private String playerName;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private BookingStatus status;
  private PaymentStatus paymentStatus;
  private BigDecimal totalPrice;

  public VendorBookingEntry() {}

  public VendorBookingEntry(UUID bookingId, String courtNumber, String playerName,
                            LocalDateTime startTime, LocalDateTime endTime,
                            BookingStatus status, PaymentStatus paymentStatus,
                            BigDecimal totalPrice) {
    this.bookingId = bookingId;
    this.courtNumber = courtNumber;
    this.playerName = playerName;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = status;
    this.paymentStatus = paymentStatus;
    this.totalPrice = totalPrice;
  }

  public UUID getBookingId() { return bookingId; }
  public void setBookingId(UUID v) { this.bookingId = v; }

  public String getCourtNumber() { return courtNumber; }
  public void setCourtNumber(String v) { this.courtNumber = v; }

  public String getPlayerName() { return playerName; }
  public void setPlayerName(String v) { this.playerName = v; }

  public LocalDateTime getStartTime() { return startTime; }
  public void setStartTime(LocalDateTime v) { this.startTime = v; }

  public LocalDateTime getEndTime() { return endTime; }
  public void setEndTime(LocalDateTime v) { this.endTime = v; }

  public BookingStatus getStatus() { return status; }
  public void setStatus(BookingStatus v) { this.status = v; }

  public PaymentStatus getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(PaymentStatus v) { this.paymentStatus = v; }

  public BigDecimal getTotalPrice() { return totalPrice; }
  public void setTotalPrice(BigDecimal v) { this.totalPrice = v; }
}
