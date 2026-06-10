package com.parth.sportsapp.sportsbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Everything the booking screen needs for one court + one day in a single call:
 * the rules (so the UI can offer 1h / 1.5h / 2h... choices) and the grid of slots.
 */
public class CourtAvailabilityResponse {

  private UUID courtId;
  private LocalDate date;

  /** null when the venue is closed that day. */
  private String openTime;   // "08:00"
  private String closeTime;  // "22:00"
  private boolean closed;

  private int slotIncrementMinutes;
  private int minBookingMinutes;
  private int maxBookingMinutes;
  private int advanceBookingDays;
  private boolean autoConfirm;

  private BigDecimal hourlyRate;

  private List<TimeSlotDto> slots;

  public UUID getCourtId() { return courtId; }
  public void setCourtId(UUID courtId) { this.courtId = courtId; }

  public LocalDate getDate() { return date; }
  public void setDate(LocalDate date) { this.date = date; }

  public String getOpenTime() { return openTime; }
  public void setOpenTime(String openTime) { this.openTime = openTime; }

  public String getCloseTime() { return closeTime; }
  public void setCloseTime(String closeTime) { this.closeTime = closeTime; }

  public boolean isClosed() { return closed; }
  public void setClosed(boolean closed) { this.closed = closed; }

  public int getSlotIncrementMinutes() { return slotIncrementMinutes; }
  public void setSlotIncrementMinutes(int slotIncrementMinutes) { this.slotIncrementMinutes = slotIncrementMinutes; }

  public int getMinBookingMinutes() { return minBookingMinutes; }
  public void setMinBookingMinutes(int minBookingMinutes) { this.minBookingMinutes = minBookingMinutes; }

  public int getMaxBookingMinutes() { return maxBookingMinutes; }
  public void setMaxBookingMinutes(int maxBookingMinutes) { this.maxBookingMinutes = maxBookingMinutes; }

  public int getAdvanceBookingDays() { return advanceBookingDays; }
  public void setAdvanceBookingDays(int advanceBookingDays) { this.advanceBookingDays = advanceBookingDays; }

  public boolean isAutoConfirm() { return autoConfirm; }
  public void setAutoConfirm(boolean autoConfirm) { this.autoConfirm = autoConfirm; }

  public BigDecimal getHourlyRate() { return hourlyRate; }
  public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

  public List<TimeSlotDto> getSlots() { return slots; }
  public void setSlots(List<TimeSlotDto> slots) { this.slots = slots; }
}
