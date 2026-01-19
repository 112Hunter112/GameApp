package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.VenueRequest;
import com.parth.sportsapp.sportsbackend.dto.VenueResponse;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.service.JwtUtil;
import com.parth.sportsapp.sportsbackend.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

  @Autowired
  private VenueService venueService;

  @Autowired
  private JwtUtil jwtUtil;

  // -----------------VENDOR-----------------

  //@ResponseBody // this just outputs in a json format

  /**
   * THIS WORKS
   * @param venueRequest
   * @param authHeader
   * @return
   */
  @PostMapping
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<VenueResponse>  CreateVenue(@Valid @RequestBody VenueRequest venueRequest,
      @RequestHeader("Authorization") String authHeader){

    UUID vendorId = getUserIdFromToken(authHeader);

    VenueResponse venueResponse = venueService.createVenue(vendorId, venueRequest);

    return ResponseEntity.status(HttpStatus.CREATED).body(venueResponse);
  }


  /**
   * This works when tested
   * @param authHeader
   * @return
   */
  @GetMapping("/my-venues")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<List<VenueResponse>> getMyVenues(
      @RequestHeader("Authorization") String authHeader) {

    UUID vendorId = getUserIdFromToken(authHeader);
    return ResponseEntity.ok(venueService.findVenueByOwnerId(vendorId));
  }


  @GetMapping("/{venueId}")
  // @PreAuthorize is NOT needed here if both Users and Vendors can view venue details
  public ResponseEntity<VenueResponse> getVenueById(@PathVariable UUID venueId) {

    // You'll need to add a simple 'getVenueById' method in your Service
    // that just calls repo.findById() and maps it to DTO.
    return ResponseEntity.ok(venueService.getVenueById(venueId));
  }

  @PutMapping("/{venueId}")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<VenueResponse> updateVenue(
      @PathVariable UUID venueId, // Getting ID from the URL (/api/venues/123)
      @Valid @RequestBody VenueRequest venueRequest,
      @RequestHeader("Authorization") String authHeader) {

    UUID vendorId = getUserIdFromToken(authHeader);

    VenueResponse venueResponse = venueService.updateVenue(venueId ,vendorId, venueRequest);

    return ResponseEntity.ok(venueResponse);
  }

  /**
   * THIS WORKS
   *
   * @param venueId
   * @param authHeader
   * @return
   */
  @DeleteMapping("/{venueId}")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<Void> deleteVenue(
      @PathVariable UUID venueId,
      @RequestHeader("Authorization") String authHeader) {

    UUID vendorId = getUserIdFromToken(authHeader);
    venueService.deleteVenue(venueId, vendorId);

    return ResponseEntity.noContent().build();
  }




  // Put this at the bottom of VenueController
  private UUID getUserIdFromToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new RuntimeException("Invalid Authorization Header");
      // GlobalExceptionHandler will catch this
    }
    String jwtToken = authHeader.substring(7);
    String userIdString = jwtUtil.extractUserId(jwtToken);
    return UUID.fromString(userIdString);
  }

  //-------------------USER-----------

  /**
   * For a get request we cant get the info from the user in a JSON format, the info is encoded
   * inside the url like /api/venues/search?lat=40.71&lon=-74.00&radius=10
   *
   * @return
   */
  @GetMapping("/search")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<Page<VenueResponse>> getVenues(@RequestParam double lat,
      @RequestParam double lon, @RequestParam(defaultValue = "10") double radius,
      Pageable pageable) {

    Page<VenueResponse> venues = venueService.searchNearbyVenues(lat, lon, radius, pageable);

    return ResponseEntity.ok(venues);
  }


  // src/main/java/com/parth/sportsapp/sportsbackend/controller/VenueController.java

  // We add 'params = "sport"' so this method ONLY runs when "sport" is in the URL
  @GetMapping(value = "/search", params = "sport")
  public ResponseEntity<List<VenueResponse>> searchVenues(@RequestParam String sport) {
    return ResponseEntity.ok(venueService.searchVenues(sport));
  }

//  @GetMapping("/nearby")
//  public List<VenueDto> searchNearby(
//      @RequestParam double lat,
//      @RequestParam double lng,
//      @RequestParam(defaultValue = "10") double radiusKm) {
//
//    // Call Service -> Repository
//    return venueService.findNearby(lat, lng, radiusKm);
//  }



  @PostMapping("/public/import")
  public ResponseEntity<VenueResponse> importPublicVenue(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String address,
      @RequestParam(required = false) Double lat,
      @RequestParam(required = false) Double lng,
      @RequestParam(required = false) String externalId
  ) {
    // Call the robust service we just built
    VenueResponse venue = venueService.getOrCreatePublicVenue(name, address, lat, lng, externalId);
    return ResponseEntity.ok(venue);
  }

}
