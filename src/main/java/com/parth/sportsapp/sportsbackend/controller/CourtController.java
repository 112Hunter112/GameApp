package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.CourtRequest;
import com.parth.sportsapp.sportsbackend.dto.CourtResponse;
import com.parth.sportsapp.sportsbackend.service.CourtService;
import com.parth.sportsapp.sportsbackend.service.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CourtController {

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private CourtService courtService;

  // -----------------VENDOR-----------------

  /**
   * When the user presses the save button when adding courts this is what will be called
   *
   * @param courtRequest
   * @param authHeader
   * @param venueId
   * @return
   */
  @PostMapping("/venues/{venueId}/courts") // <--- 1. Add the URL path here
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<CourtResponse> createCourt(@Valid @RequestBody CourtRequest courtRequest,
      @RequestHeader("Authorization") String authHeader, @PathVariable UUID venueId){

    UUID vendorId =  getUserIdFromToken(authHeader);
    CourtResponse courtResponse = courtService.addCourt(venueId, vendorId, courtRequest);

    return ResponseEntity.status(HttpStatus.CREATED).body(courtResponse);
  }

  @PutMapping("/courts/{courtId}")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<CourtResponse> updateCourt(
      @PathVariable UUID courtId,
      @Valid @RequestBody CourtRequest request,
      @RequestHeader("Authorization") String authHeader) {

    UUID vendorId = getUserIdFromToken(authHeader);
    return ResponseEntity.ok(courtService.updateCourt(courtId, vendorId, request));
  }

  @DeleteMapping("/courts/{courtId}")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<Void> deleteCourt(
      @PathVariable UUID courtId,
      @RequestHeader("Authorization") String authHeader) {

    UUID vendorId = getUserIdFromToken(authHeader);
    courtService.deleteCourt(courtId, vendorId);
    return ResponseEntity.noContent().build();
  }



  @GetMapping("/my-courts")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<List<CourtResponse>> getMyCourts(
      @RequestHeader("Authorization") String authHeader) {

    UUID vendorId = getUserIdFromToken(authHeader);
    return ResponseEntity.ok(courtService.getVendorCourts(vendorId));
  }

  // GET /api/venues/{venueId}/courts?sportId=...
  @GetMapping("/venues/{venueId}/courts")
  public ResponseEntity<List<CourtResponse>> getCourtsByVenue(
      @PathVariable UUID venueId,
      @RequestParam(required = false) UUID sportId) { // Optional!

    if (sportId != null) {
      // If user specifically asked for "Tennis" at this venue
      return ResponseEntity.ok(courtService.getCourtsByVenueAndSport(venueId, sportId));
    } else {
      // Otherwise, show everything (Basketball + Tennis)
      return ResponseEntity.ok(courtService.getCourtsByVenue(venueId));
    }
  }

  @GetMapping("/courts/{courtId}")
  public ResponseEntity<CourtResponse> getCourtById(@PathVariable UUID courtId) {
    return ResponseEntity.ok(courtService.getCourtById(courtId));
  }


  // GET /api/courts/sport/{sportId}
  @GetMapping("/courts/sport/{sportId}")
  public ResponseEntity<List<CourtResponse>> getCourtsBySport(@PathVariable UUID sportId) {

    // 1. Call your new service method
    List<CourtResponse> courts = courtService.getCourtsBySport(sportId);

    // 2. Return the list
    return ResponseEntity.ok(courts);
  }



  private UUID getUserIdFromToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new RuntimeException("Invalid Authorization Header");
      // GlobalExceptionHandler will catch this
    }
    String jwtToken = authHeader.substring(7);
    String userIdString = jwtUtil.extractUserId(jwtToken);
    return UUID.fromString(userIdString);
  }

}
