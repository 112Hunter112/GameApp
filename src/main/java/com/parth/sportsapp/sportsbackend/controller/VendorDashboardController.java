package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.VendorBookingEntry;
import com.parth.sportsapp.sportsbackend.dto.VendorDashboardResponse;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.VendorDashboardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The venue owner's "booking manager" API.
 *
 * <pre>
 *   GET  /api/vendor/venues/{venueId}/dashboard            — money/activity/leakage snapshot
 *   GET  /api/vendor/venues/{venueId}/schedule?from=&to=   — paginated booking calendar
 *   POST /api/vendor/bookings/{bookingId}/no-show          — mark a past booking as no-show
 * </pre>
 *
 * All endpoints require the VENUE_OWNER role; per-venue ownership is verified
 * in the service (a vendor can never read another vendor's venue).
 */
@RestController
@RequestMapping("/api/vendor")
@Validated
@PreAuthorize("hasRole('VENUE_OWNER')")
public class VendorDashboardController {

  private final VendorDashboardService dashboardService;
  private final com.parth.sportsapp.sportsbackend.service.SmartFillService smartFillService;

  public VendorDashboardController(
      VendorDashboardService dashboardService,
      com.parth.sportsapp.sportsbackend.service.SmartFillService smartFillService) {
    this.dashboardService = dashboardService;
    this.smartFillService = smartFillService;
  }

  @GetMapping("/venues/{venueId}/dashboard")
  public VendorDashboardResponse dashboard(
      @AuthenticationPrincipal User vendor,
      @PathVariable UUID venueId) {
    return dashboardService.getDashboard(vendor.getId(), venueId);
  }

  @GetMapping("/venues/{venueId}/schedule")
  public Page<VendorBookingEntry> schedule(
      @AuthenticationPrincipal User vendor,
      @PathVariable UUID venueId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      Pageable pageable) {
    return dashboardService.getSchedule(vendor.getId(), venueId, from, to, pageable);
  }

  @PostMapping("/bookings/{bookingId}/no-show")
  public ResponseEntity<Void> markNoShow(
      @AuthenticationPrincipal User vendor,
      @PathVariable UUID bookingId) {
    dashboardService.markNoShow(vendor.getId(), bookingId);
    return ResponseEntity.noContent().build();
  }

  /**
   * "Revenue left on the table": per-court empty hours priced at the court's
   * hourly rate over the trailing window. The pitch-number report.
   */
  @GetMapping("/venues/{venueId}/revenue-report")
  public com.parth.sportsapp.sportsbackend.dto.RevenueReportResponse revenueReport(
      @AuthenticationPrincipal User vendor,
      @PathVariable UUID venueId,
      @RequestParam(defaultValue = "30") int windowDays,
      @RequestParam(defaultValue = "12") int openHoursPerDay) {
    return dashboardService.getRevenueReport(vendor.getId(), venueId, windowDays, openHoursPerDay);
  }

  /**
   * Smart Fill: offer an empty slot to nearby matching players. Returns the
   * number of players notified.
   */
  @PostMapping("/courts/{courtId}/smart-fill")
  public ResponseEntity<java.util.Map<String, Object>> smartFill(
      @AuthenticationPrincipal User vendor,
      @PathVariable UUID courtId,
      @jakarta.validation.Valid @RequestBody com.parth.sportsapp.sportsbackend.dto.SmartFillRequest request) {
    int notified = smartFillService.offerSlot(vendor.getId(), courtId, request);
    return ResponseEntity.ok(java.util.Map.of("playersNotified", notified));
  }

  /** Cross-venue rollup for owners with multiple venues. */
  @GetMapping("/dashboard")
  public com.parth.sportsapp.sportsbackend.dto.OwnerOverviewResponse ownerOverview(
      @AuthenticationPrincipal User vendor) {
    return dashboardService.getOwnerOverview(vendor.getId());
  }

  /**
   * Hour-of-week occupancy heatmap (ISO day 1=Mon..7=Sun x hour 0-23).
   * The grid that shows the vendor WHERE their dead hours are.
   */
  @GetMapping("/venues/{venueId}/heatmap")
  public com.parth.sportsapp.sportsbackend.dto.HeatmapResponse heatmap(
      @AuthenticationPrincipal User vendor,
      @PathVariable UUID venueId,
      @RequestParam(defaultValue = "30") int windowDays) {
    return dashboardService.getHeatmap(vendor.getId(), venueId, windowDays);
  }

  /** CSV export of the booking schedule, for accounting. */
  @GetMapping(value = "/venues/{venueId}/bookings.csv", produces = "text/csv")
  public ResponseEntity<String> exportCsv(
      @AuthenticationPrincipal User vendor,
      @PathVariable UUID venueId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
    String csv = dashboardService.exportBookingsCsv(vendor.getId(), venueId, from, to);
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"bookings.csv\"")
        .body(csv);
  }
}
