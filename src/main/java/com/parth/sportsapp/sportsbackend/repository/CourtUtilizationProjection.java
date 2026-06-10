package com.parth.sportsapp.sportsbackend.repository;

import java.util.UUID;

/**
 * One row of the per-court utilization aggregate. Aliases in the native query
 * (court_id, court_number, booking_count, booked_hours) bind to these getters
 * via snake_case → camelCase collapsing.
 */
public interface CourtUtilizationProjection {
  UUID getCourtId();
  String getCourtNumber();
  java.math.BigDecimal getHourlyRate();
  Long getBookingCount();
  Double getBookedHours();
}
