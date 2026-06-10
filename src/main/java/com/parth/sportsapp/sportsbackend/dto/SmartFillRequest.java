package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Vendor's "fill this empty slot" request. The vendor names the slot and an
 * optional discount; the platform finds matching nearby players and notifies
 * them. No booking is created here — players book through the normal flow.
 */
public class SmartFillRequest {

  @NotNull(message = "slotStart is required")
  private LocalDateTime slotStart;

  @NotNull(message = "slotEnd is required")
  private LocalDateTime slotEnd;

  /** Optional discount to advertise, e.g. 30 = "30% off". */
  @Min(0) @Max(90)
  private Integer discountPercent;

  public SmartFillRequest() {}

  public LocalDateTime getSlotStart() { return slotStart; }
  public void setSlotStart(LocalDateTime v) { this.slotStart = v; }

  public LocalDateTime getSlotEnd() { return slotEnd; }
  public void setSlotEnd(LocalDateTime v) { this.slotEnd = v; }

  public Integer getDiscountPercent() { return discountPercent; }
  public void setDiscountPercent(Integer v) { this.discountPercent = v; }
}
