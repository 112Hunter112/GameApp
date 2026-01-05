package com.parth.sportsapp.sportsbackend.service;


import com.parth.sportsapp.sportsbackend.dto.VenueRequest;
import com.parth.sportsapp.sportsbackend.dto.VenueResponse;
import com.parth.sportsapp.sportsbackend.mapper.VenueMapper;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import com.parth.sportsapp.sportsbackend.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable; // <--- ADD THIS



/*
"My Venues" Page: You return a list of Venues (Names & Addresses only).

Vendor sees: "Downtown Arena", "Uptown Gym".

The Click: The Vendor clicks on "Downtown Arena" (ID: 100).

The Request: Frontend calls GET /api/venues/100/courts.

The Service: Calls courtRepository.findByVenueId(100).

The Result: Returns [Court 1, Court 2].
 */




@Service
@Transactional
public class VenueService {

  @Autowired
  private VenueRepository venueRepository;

  @Autowired
  private VenueMapper venueMapper;

@Autowired
private UserRepository userRepository;

  private static final double KM_TO_METERS = 1000.0;
  private static final double MAX_SEARCH_RADIUS_KM = 50.0;
  private static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;



  // ----------------VENDOR SIDE ------------------

  //list all venues for the vendor after verifying that it is a VENDOR
  public List<VenueResponse> findVenueByOwnerId(UUID vendorId) {

    // checks to see if the vendor exists, this implicitly chekcs if person is a VENDOR
    User vendor = userRepository.findById(vendorId)
        .orElseThrow(() -> new RuntimeException("Vendor not found"));

    List<Venue> venues = venueRepository.findByOwner_Id(vendorId);

    return venueMapper.toResponseList(venues);
  }

  /**
   *
   * @param vendorId the UUID the of the user from the jwt
   * @param venueRequest the data the user entered and then pressed save
   * @return a saved Venue of the data the user entered on the frontend
   */
  public VenueResponse createVenue(UUID vendorId, VenueRequest venueRequest) {
    // Opens a list to fill out which the user does and when they press save we call this method

    // 0.1 Checks that this person passing it through has a valid ID and in VENDOR owner list
    User vendor = userRepository.findById(vendorId)
        .orElseThrow(() -> new RuntimeException("Vendor not found"));

    Venue venueEntity = venueMapper.toEntity(venueRequest, vendor);



    // 1. Unique name, address, phone number, etc.
    validateVenueInfo(venueEntity);

    // 2. Check for Duplicates
    // Check for duplicates (always, since it's a new venue)
    boolean exists = venueRepository.existsByNameIgnoreCaseAndAddressIgnoreCase(
        venueRequest.getName(),
        venueRequest.getAddress()
    );

    if (exists) {
      throw new RuntimeException("A venue with this name and address already exists!");
    }

    Venue savedVenue = venueRepository.save(venueEntity);

    return venueMapper.toResponse(savedVenue);
  }

  /**
   * once a user clicks on the venue they get a venueId for the venue they clicked on
   *
   *
   * @param venueId this is the ID of the box the user clicked on
   * @param vendorId this comes from the UUID
   * @param updateRequest
   * @return
   */
  public VenueResponse updateVenue(UUID venueId,  UUID vendorId, VenueRequest updateRequest) {

 // 0.1 check if venue exists using venueId, before we update, has not been compared to
    // vendors' current active venue
    Venue existingVenue = venueRepository.findById(venueId)
        .orElseThrow(() -> new RuntimeException("Venue not found with ID: " + venueId));

    //0.2 Check if the vendor ID is the same as venueID's owner we want to update
    if (!existingVenue.getOwner().getId().equals(vendorId)) {
      throw new RuntimeException("ACCESS DENIED: You do not own this venue.");
    }


    // THIS PART IS DONE BY THE MAPPER NOW
//    if (updateRequest.getLatitude() != null && updateRequest.getLongitude() != null) {
//      org.locationtech.jts.geom.Point point = geometryFactory.createPoint(
//          new org.locationtech.jts.geom.Coordinate(updateRequest.getLongitude(), updateRequest.getLatitude())
//      );
//      updateRequest.setLocation(point);
//    }

    // Validate AFTER updating
    venueMapper.updateEntity(existingVenue, updateRequest);

    validateVenueInfo(existingVenue);  // Now validate the updated entity

    Venue savedVenue = venueRepository.save(existingVenue);

    return venueMapper.toResponse(savedVenue);
  }



  /**
   *
   * @param venueId ID of the venue we want to delete
   * @param vendorID this is the vendors whos info we wnt to dlete
   */
  public void deleteVenue(UUID venueId, UUID vendorID) {
     //turn the isActive to false

    User vendor = userRepository.findById(vendorID)
        .orElseThrow(() -> new RuntimeException("Vendor not found"));

    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    if (!venue.getOwner().getId().equals(vendorID)) {
      throw new RuntimeException("ACCESS DENIED: You cannot delete a venue you don't own.");
    }

    venue.setActive(false);

    venueRepository.save(venue);

  }

  private void validateVenueInfo(Venue venue) {
    // --- 1. STRING VALIDATIONS ---
    if (venue.getName() == null || venue.getName().trim().isEmpty()) {
      throw new RuntimeException("Venue name is required.");
    }
    if (venue.getAddress() == null || venue.getAddress().trim().isEmpty()) {
      throw new RuntimeException("Address is required.");
    }
    if (venue.getPhoneNumber() == null || venue.getPhoneNumber().trim().isEmpty()) {
      throw new RuntimeException("Phone number is required.");
    }
    // Basic Phone Regex (10-15 digits, allows optional +)
    if (!venue.getPhoneNumber().matches("^\\+?[0-9]{10,15}$")) {
      throw new RuntimeException("Invalid phone number format.");
    }

    // --- 2. LOCATION VALIDATION (Crucial for Maps) ---
    // If the frontend didn't send coordinates, your app's "Search Nearby" will break.
    if (venue.getLocation() == null) {
      throw new RuntimeException("Venue location (latitude/longitude) is required.");
    }
    // Optional: Bounds Check (Latitude -90 to 90, Longitude -180 to 180)
    double lat = venue.getLocation().getY();
    double lng = venue.getLocation().getX();
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      throw new RuntimeException("Invalid coordinates provided.");
    }

    // --- 3. OPENING HOURS VALIDATION ---
    // You don't want a venue with no hours defined.
    if (venue.getOpeningHours() == null || venue.getOpeningHours().isEmpty()) {
      throw new RuntimeException("Opening hours are required.");
    }

    // --- 4. AMENITIES SAFETY ---
    // Prevent NullPointerExceptions later. If null, make it an empty list.
    if (venue.getAmenities() == null) {
      venue.setAmenities(List.of());
    }

  }

  /**
   * Get single venue by ID (for detail page)
   */
  public VenueResponse getVenueById(UUID venueId) {
    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    // Only show active venues to users (optional - depends on your business logic)
    if (!venue.isActive()) {
      throw new RuntimeException("Venue not found");
    }

    return venueMapper.toResponse(venue);
  }


  // ----------------USER SIDE ------------------

  public Page<VenueResponse> searchNearbyVenues(double lat, double lon, double radiusKm, Pageable pageable) {
    // 1. Validate Coordinates
    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
      throw new RuntimeException("Invalid latitude or longitude");
    }

    // 2. [FIX] Validate Radius and capture the CLEAN value
    // This ensures we use the logic inside 'validateRadius' (max 50km, etc.)
    double validRadiusKm = validateRadius(radiusKm);

    // 3. Convert valid radius to Meters
    double radiusMeters = validRadiusKm * KM_TO_METERS;

    // 4. Fetch Page
    Page<Venue> venuePage = venueRepository.findVenuesNearby(lat, lon, radiusMeters, pageable);

    // 5. Map to DTO
    return venuePage.map(venue -> venueMapper.toResponse(venue));
  }

  private double validateRadius(Double radiusKm) {
    if (radiusKm == null) {
      return DEFAULT_SEARCH_RADIUS_KM;
    }
    if (radiusKm <= 0) {
      throw new IllegalArgumentException("Radius must be positive");
    }
    if (radiusKm > MAX_SEARCH_RADIUS_KM) {
      throw new IllegalArgumentException(
          "Radius cannot exceed " + MAX_SEARCH_RADIUS_KM + " km"
      );
    }
    return radiusKm;
  }

  // src/main/java/com/parth/sportsapp/sportsbackend/service/VenueService.java

  public List<VenueResponse> searchVenues(String sportName) {
    List<Venue> venues;
    if (sportName == null || sportName.isBlank()) {
      venues = venueRepository.findAll(); // Or findByIsActiveTrue()
    } else {
      venues = venueRepository.findBySportName(sportName);
    }
    return venues.stream().map(venueMapper::toResponse).collect(Collectors.toList());
  }



}
