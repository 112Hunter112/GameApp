package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/** Venue owner's booking-rules form. All fields required; openingHours key = DayOfWeek name. */
public class BookingPolicyRequest {

  /** 30 or 60. */
  @NotNull
  private Integer slotIncrementMinutes;

  @NotNull
  @Min(value = 30, message = "Minimum booking must be at least 30 minutes")
  private Integer minBookingMinutes;

  @NotNull
  @Max(value = 480, message = "Maximum booking cannot exceed 8 hours")
  private Integer maxBookingMinutes;

  @NotNull
  @Min(value = 1, message = "Advance booking window must be at least 1 day")
  @Max(value = 90, message = "Advance booking window cannot exceed 90 days")
  private Integer advanceBookingDays;

  @NotNull
  @Min(value = 0)
  @Max(value = 1440, message = "Minimum notice cannot exceed 24 hours")
  private Integer minNoticeMinutes;

  @NotNull
  @Min(value = 0)
  @Max(value = 10080, message = "Cancellation cutoff cannot exceed 7 days")
  private Integer cancellationCutoffMinutes;

  @NotNull
  private Boolean autoConfirm;

  /** e.g. {"MONDAY": "08:00-22:00"}. Omit a day to mark it closed. */
  @NotNull
  private Map<String, String> openingHours;

  public Integer getSlotIncrementMinutes() { return slotIncrementMinutes; }
  public void setSlotIncrementMinutes(Integer slotIncrementMinutes) { this.slotIncrementMinutes = slotIncrementMinutes; }

  public Integer getMinBookingMinutes() { return minBookingMinutes; }
  public void setMinBookingMinutes(Integer minBookingMinutes) { this.minBookingMinutes = minBookingMinutes; }

  public Integer getMaxBookingMinutes() { return maxBookingMinutes; }
  public void setMaxBookingMinutes(Integer maxBookingMinutes) { this.maxBookingMinutes = maxBookingMinutes; }

  public Integer getAdvanceBookingDays() { return advanceBookingDays; }
  public void setAdvanceBookingDays(Integer advanceBookingDays) { this.advanceBookingDays = advanceBookingDays; }

  public Integer getMinNoticeMinutes() { return minNoticeMinutes; }
  public void setMinNoticeMinutes(Integer minNoticeMinutes) { this.minNoticeMinutes = minNoticeMinutes; }

  public Integer getCancellationCutoffMinutes() { return cancellationCutoffMinutes; }
  public void setCancellationCutoffMinutes(Integer cancellationCutoffMinutes) { this.cancellationCutoffMinutes = cancellationCutoffMinutes; }

  public Boolean getAutoConfirm() { return autoConfirm; }
  public void setAutoConfirm(Boolean autoConfirm) { this.autoConfirm = autoConfirm; }

  public Map<String, String> getOpeningHours() { return openingHours; }
  public void setOpeningHours(Map<String, String> openingHours) { this.openingHours = openingHours; }
}
