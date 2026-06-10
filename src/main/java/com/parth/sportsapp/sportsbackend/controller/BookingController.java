package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.BookingPolicyRequest;
import com.parth.sportsapp.sportsbackend.dto.BookingPolicyResponse;
import com.parth.sportsapp.sportsbackend.dto.BookingRequest;
import com.parth.sportsapp.sportsbackend.dto.BookingResponse;
import com.parth.sportsapp.sportsbackend.dto.CourtAvailabilityResponse;
import com.parth.sportsapp.sportsbackend.model.BookingStatus;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.BookingPolicyService;
import com.parth.sportsapp.sportsbackend.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Court booking API.
 *
 * Player flow:
 *   GET  /api/courts/{courtId}/availability?date=2026-06-12   (public — browse before login)
 *   POST /api/bookings                                        (book / request-to-book)
 *   GET  /api/bookings/me?scope=upcoming|past
 *   GET  /api/bookings/{id}
 *   POST /api/bookings/{id}/cancel
 *
 * Venue-owner flow:
 *   GET  /api/owner/bookings?venueId=&date=&status=
 *   GET  /api/owner/bookings/pending-count
 *   POST /api/bookings/{id}/confirm
 *   POST /api/bookings/{id}/decline
 *   GET  /api/venues/{venueId}/booking-policy                 (public — booking screen needs it)
 *   PUT  /api/venues/{venueId}/booking-policy
 */
@RestController
@RequestMapping("/api")
public class BookingController {

  @Autowired
  private BookingService bookingService;

  @Autowired
  private BookingPolicyService bookingPolicyService;

  // ===========================================================================
  // AVAILABILITY (public)
  // ===========================================================================

  @GetMapping("/courts/{courtId}/availability")
  public ResponseEntity<CourtAvailabilityResponse> getAvailability(
      @PathVariable UUID courtId,
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(bookingService.getAvailability(courtId, date));
  }

  // ===========================================================================
  // PLAYER
  // ===========================================================================

  @PostMapping("/bookings")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BookingResponse> createBooking(
      @AuthenticationPrincipal User user,
      @Valid @RequestBody BookingRequest request) {
    BookingResponse response = bookingService.createBooking(user.getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/bookings/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<BookingResponse>> getMyBookings(
      @AuthenticationPrincipal User user,
      @RequestParam(name = "scope", defaultValue = "upcoming") String scope,
      Pageable pageable) {
    Page<BookingResponse> page = "past".equalsIgnoreCase(scope)
        ? bookingService.getMyPast(user.getId(), pageable)
        : bookingService.getMyUpcoming(user.getId(), pageable);
    return ResponseEntity.ok(page);
  }

  @GetMapping("/bookings/{bookingId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BookingResponse> getBooking(
      @AuthenticationPrincipal User user,
      @PathVariable UUID bookingId) {
    return ResponseEntity.ok(bookingService.getBooking(bookingId, user.getId()));
  }

  /** Cancel — works for the player who booked AND for the venue owner (with optional reason). */
  @PostMapping("/bookings/{bookingId}/cancel")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<BookingResponse> cancelBooking(
      @AuthenticationPrincipal User user,
      @PathVariable UUID bookingId,
      @RequestBody(required = false) Map<String, String> body) {
    String reason = body != null ? body.get("reason") : null;
    return ResponseEntity.ok(bookingService.cancelBooking(bookingId, user.getId(), reason));
  }

  // ===========================================================================
  // VENUE OWNER
  // ===========================================================================

  @GetMapping("/owner/bookings")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<Page<BookingResponse>> getOwnerBookings(
      @AuthenticationPrincipal User user,
      @RequestParam(name = "venueId", required = false) UUID venueId,
      @RequestParam(name = "date", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(name = "status", required = false) BookingStatus status,
      Pageable pageable) {
    return ResponseEntity.ok(
        bookingService.getOwnerBookings(user.getId(), venueId, date, status, pageable));
  }

  @GetMapping("/owner/bookings/pending-count")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<Map<String, Long>> getPendingCount(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(Map.of("pendingCount", bookingService.getOwnerPendingCount(user.getId())));
  }

  @PostMapping("/bookings/{bookingId}/confirm")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<BookingResponse> confirmBooking(
      @AuthenticationPrincipal User user,
      @PathVariable UUID bookingId) {
    return ResponseEntity.ok(bookingService.confirmBooking(bookingId, user.getId()));
  }

  @PostMapping("/bookings/{bookingId}/decline")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<BookingResponse> declineBooking(
      @AuthenticationPrincipal User user,
      @PathVariable UUID bookingId,
      @RequestBody(required = false) Map<String, String> body) {
    String reason = body != null ? body.get("reason") : null;
    return ResponseEntity.ok(bookingService.declineBooking(bookingId, user.getId(), reason));
  }

  // ===========================================================================
  // BOOKING POLICY
  // ===========================================================================

  /** Public: the booking screen shows rules (instant book, durations) before login. */
  @GetMapping("/venues/{venueId}/booking-policy")
  public ResponseEntity<BookingPolicyResponse> getBookingPolicy(@PathVariable UUID venueId) {
    return ResponseEntity.ok(bookingPolicyService.getPolicy(venueId));
  }

  @PutMapping("/venues/{venueId}/booking-policy")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<BookingPolicyResponse> updateBookingPolicy(
      @AuthenticationPrincipal User user,
      @PathVariable UUID venueId,
      @Valid @RequestBody BookingPolicyRequest request) {
    return ResponseEntity.ok(bookingPolicyService.updatePolicy(venueId, user.getId(), request));
  }
}
