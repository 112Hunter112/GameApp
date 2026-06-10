package com.parth.sportsapp.sportsbackend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The "revenue left on the table" report — the sales-pitch number.
 *
 * <p>For each court over the window: how many bookable hours existed, how many
 * were actually booked, and what the empty ones would have earned at the
 * court's hourly rate. The estimate is deliberately simple and labelled as
 * such — its job is to make under-utilization visible, not to be an
 * accounting document.</p>
 */
public class RevenueReportResponse {

  private UUID venueId;
  private String venueName;
  private int windowDays;
  /** Assumed bookable hours per day used in the estimate (configurable). */
  private int assumedOpenHoursPerDay;

  private BigDecimal totalMissedRevenue;
  private double totalEmptyHours;

  private List<CourtRevenueRow> courts;

  public RevenueReportResponse() {}

  public UUID getVenueId() { return venueId; }
  public void setVenueId(UUID v) { this.venueId = v; }

  public String getVenueName() { return venueName; }
  public void setVenueName(String v) { this.venueName = v; }

  public int getWindowDays() { return windowDays; }
  public void setWindowDays(int v) { this.windowDays = v; }

  public int getAssumedOpenHoursPerDay() { return assumedOpenHoursPerDay; }
  public void setAssumedOpenHoursPerDay(int v) { this.assumedOpenHoursPerDay = v; }

  public BigDecimal getTotalMissedRevenue() { return totalMissedRevenue; }
  public void setTotalMissedRevenue(BigDecimal v) { this.totalMissedRevenue = v; }

  public double getTotalEmptyHours() { return totalEmptyHours; }
  public void setTotalEmptyHours(double v) { this.totalEmptyHours = v; }

  public List<CourtRevenueRow> getCourts() { return courts; }
  public void setCourts(List<CourtRevenueRow> v) { this.courts = v; }

  public static class CourtRevenueRow {
    private UUID courtId;
    private String courtNumber;
    private BigDecimal hourlyRate;
    private double bookedHours;
    private double emptyHours;
    private double utilizationPct;       // 0-100
    private BigDecimal missedRevenue;

    public CourtRevenueRow() {}

    public UUID getCourtId() { return courtId; }
    public void setCourtId(UUID v) { this.courtId = v; }

    public String getCourtNumber() { return courtNumber; }
    public void setCourtNumber(String v) { this.courtNumber = v; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal v) { this.hourlyRate = v; }

    public double getBookedHours() { return bookedHours; }
    public void setBookedHours(double v) { this.bookedHours = v; }

    public double getEmptyHours() { return emptyHours; }
    public void setEmptyHours(double v) { this.emptyHours = v; }

    public double getUtilizationPct() { return utilizationPct; }
    public void setUtilizationPct(double v) { this.utilizationPct = v; }

    public BigDecimal getMissedRevenue() { return missedRevenue; }
    public void setMissedRevenue(BigDecimal v) { this.missedRevenue = v; }
  }
}
