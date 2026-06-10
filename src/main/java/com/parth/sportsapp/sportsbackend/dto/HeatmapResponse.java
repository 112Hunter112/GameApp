package com.parth.sportsapp.sportsbackend.dto;

import java.util.List;
import java.util.UUID;

/**
 * Hour-of-week occupancy grid for a venue. Cells with zero bookings are
 * omitted — the frontend treats missing cells as empty (those are the
 * Smart Fill targets).
 */
public class HeatmapResponse {

  private UUID venueId;
  private int windowDays;
  private List<Cell> cells;

  public HeatmapResponse() {}

  public HeatmapResponse(UUID venueId, int windowDays, List<Cell> cells) {
    this.venueId = venueId;
    this.windowDays = windowDays;
    this.cells = cells;
  }

  public UUID getVenueId() { return venueId; }
  public void setVenueId(UUID v) { this.venueId = v; }

  public int getWindowDays() { return windowDays; }
  public void setWindowDays(int v) { this.windowDays = v; }

  public List<Cell> getCells() { return cells; }
  public void setCells(List<Cell> v) { this.cells = v; }

  public static class Cell {
    /** ISO day of week: 1 = Monday … 7 = Sunday. */
    private int dayOfWeek;
    /** Hour of day 0-23. */
    private int hourOfDay;
    private long bookingCount;

    public Cell() {}

    public Cell(int dayOfWeek, int hourOfDay, long bookingCount) {
      this.dayOfWeek = dayOfWeek;
      this.hourOfDay = hourOfDay;
      this.bookingCount = bookingCount;
    }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int v) { this.dayOfWeek = v; }

    public int getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(int v) { this.hourOfDay = v; }

    public long getBookingCount() { return bookingCount; }
    public void setBookingCount(long v) { this.bookingCount = v; }
  }
}
