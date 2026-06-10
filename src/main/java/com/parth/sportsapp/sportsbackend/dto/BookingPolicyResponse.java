package com.parth.sportsapp.sportsbackend.dto;

import java.util.Map;
import java.util.UUID;

public class BookingPolicyResponse {

  private UUID venueId;
  private int slotIncrementMinutes;
  private int minBookingMinutes;
  private int maxBookingMinutes;
  private int advanceBookingDays;
  private int minNoticeMinutes;
  private int cancellationCutoffMinutes;
  private boolean autoConfirm;
  private Map<String, String> openingHours;

  /** false when the venue is still on platform defaults (owner never saved a policy). */
  private boolean customized;

  public UUID getVenueId() { return venueId; }
  public void setVenueId(UUID venueId) { this.venueId = venueId; }

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

  public boolean isCustomized() { return customized; }
  public void setCustomized(boolean customized) { this.customized = customized; }
}
