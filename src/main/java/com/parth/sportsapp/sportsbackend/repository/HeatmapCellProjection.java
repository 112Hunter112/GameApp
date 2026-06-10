package com.parth.sportsapp.sportsbackend.repository;

/** One (day-of-week, hour, count) cell of the occupancy heatmap. */
public interface HeatmapCellProjection {
  /** ISO day of week: 1 = Monday … 7 = Sunday. */
  Integer getDayOfWeek();
  /** Hour of day 0-23. */
  Integer getHourOfDay();
  Long getBookingCount();
}
