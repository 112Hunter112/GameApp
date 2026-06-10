package com.parth.sportsapp.sportsbackend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Cross-venue rollup for owners with more than one venue: combined totals
 * plus a per-venue breakdown sorted by monthly revenue.
 */
public class OwnerOverviewResponse {

  private int totalVenues;
  private BigDecimal revenueThisWeek;
  private BigDecimal revenueThisMonth;
  private long bookingsToday;
  private long noShowsLast30Days;
  private long cancellationsLast30Days;
  private List<VenueSummary> venues;

  public OwnerOverviewResponse() {}

  public int getTotalVenues() { return totalVenues; }
  public void setTotalVenues(int v) { this.totalVenues = v; }

  public BigDecimal getRevenueThisWeek() { return revenueThisWeek; }
  public void setRevenueThisWeek(BigDecimal v) { this.revenueThisWeek = v; }

  public BigDecimal getRevenueThisMonth() { return revenueThisMonth; }
  public void setRevenueThisMonth(BigDecimal v) { this.revenueThisMonth = v; }

  public long getBookingsToday() { return bookingsToday; }
  public void setBookingsToday(long v) { this.bookingsToday = v; }

  public long getNoShowsLast30Days() { return noShowsLast30Days; }
  public void setNoShowsLast30Days(long v) { this.noShowsLast30Days = v; }

  public long getCancellationsLast30Days() { return cancellationsLast30Days; }
  public void setCancellationsLast30Days(long v) { this.cancellationsLast30Days = v; }

  public List<VenueSummary> getVenues() { return venues; }
  public void setVenues(List<VenueSummary> v) { this.venues = v; }

  public static class VenueSummary {
    private UUID venueId;
    private String venueName;
    private BigDecimal revenueThisMonth;
    private long bookingsToday;

    public VenueSummary() {}

    public UUID getVenueId() { return venueId; }
    public void setVenueId(UUID v) { this.venueId = v; }

    public String getVenueName() { return venueName; }
    public void setVenueName(String v) { this.venueName = v; }

    public BigDecimal getRevenueThisMonth() { return revenueThisMonth; }
    public void setRevenueThisMonth(BigDecimal v) { this.revenueThisMonth = v; }

    public long getBookingsToday() { return bookingsToday; }
    public void setBookingsToday(long v) { this.bookingsToday = v; }
  }
}
