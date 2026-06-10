package com.parth.sportsapp.sportsbackend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The venue owner's operational snapshot — one call powers the manager
 * dashboard screen:
 *
 * <ul>
 *   <li>money: revenue this week / this month</li>
 *   <li>activity: today's booking count, the upcoming schedule</li>
 *   <li>leakage: no-shows and cancellations in the last 30 days</li>
 *   <li>utilization: booked hours per court (empty courts = lost revenue)</li>
 * </ul>
 */
public class VendorDashboardResponse {

  private UUID venueId;
  private String venueName;

  private BigDecimal revenueThisWeek;
  private BigDecimal revenueThisMonth;

  private long bookingsToday;
  private long noShowsLast30Days;
  private long cancellationsLast30Days;

  private List<VendorBookingEntry> upcomingBookings;
  private List<CourtUtilizationDto> courtUtilization;

  public VendorDashboardResponse() {}

  public UUID getVenueId() { return venueId; }
  public void setVenueId(UUID v) { this.venueId = v; }

  public String getVenueName() { return venueName; }
  public void setVenueName(String v) { this.venueName = v; }

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

  public List<VendorBookingEntry> getUpcomingBookings() { return upcomingBookings; }
  public void setUpcomingBookings(List<VendorBookingEntry> v) { this.upcomingBookings = v; }

  public List<CourtUtilizationDto> getCourtUtilization() { return courtUtilization; }
  public void setCourtUtilization(List<CourtUtilizationDto> v) { this.courtUtilization = v; }

  /** Per-court slice of the utilization table. */
  public static class CourtUtilizationDto {
    private UUID courtId;
    private String courtNumber;
    private long bookingCount;
    private double bookedHours;

    public CourtUtilizationDto() {}

    public CourtUtilizationDto(UUID courtId, String courtNumber,
                               long bookingCount, double bookedHours) {
      this.courtId = courtId;
      this.courtNumber = courtNumber;
      this.bookingCount = bookingCount;
      this.bookedHours = bookedHours;
    }

    public UUID getCourtId() { return courtId; }
    public void setCourtId(UUID v) { this.courtId = v; }

    public String getCourtNumber() { return courtNumber; }
    public void setCourtNumber(String v) { this.courtNumber = v; }

    public long getBookingCount() { return bookingCount; }
    public void setBookingCount(long v) { this.bookingCount = v; }

    public double getBookedHours() { return bookedHours; }
    public void setBookedHours(double v) { this.bookedHours = v; }
  }
}
