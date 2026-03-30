package com.parth.sportsapp.sportsbackend.service;


import com.parth.sportsapp.sportsbackend.dto.VenueRequest;
import com.parth.sportsapp.sportsbackend.dto.VenueResponse;
import com.parth.sportsapp.sportsbackend.mapper.VenueMapper;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.model.VenueSource;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import com.parth.sportsapp.sportsbackend.repository.VenueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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

  private static final Logger logger = LoggerFactory.getLogger(VenueService.class);

  private static final double KM_TO_METERS = 1000.0;
  private static final double MAX_SEARCH_RADIUS_KM = 50.0;
  private static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;
  private static final double MIN_SEARCH_RADIUS_M = 20.0;
  private static final double MAX_SEARCH_RADIUS_M = 200.0;

  private static final String SYSTEM_ADMIN_EMAIL = "public_venues@sportsapp.com";



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
    if (venue.getLatitude() == null || venue.getLongitude() == null) {
      throw new RuntimeException("Venue location (latitude/longitude) is required.");
    }
    double lat = venue.getLatitude();
    double lng = venue.getLongitude();
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


  /**
   * A method that will transition existing non-vendor Venues into the hands of the vendor if the
   * venue actually belongs to them.
   *
   * todo Finish this later
   *
   */


  // ----------------USER SIDE ------------------

  public Page<VenueResponse> searchNearbyVenues(double lat, double lon, double radiusKm, Pageable pageable) {
    // 1. Validate Coordinates
    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
      throw new RuntimeException("Invalid latitude or longitude");
    }

    // 2. Validate Radius and capture the CLEAN value
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


  // This is th format of the ORM output from the manual match end
//  {
//    "place_id": 123456,                // Map this to -> externalId
//      "lat": "40.7829",                  // Map this to -> lat
//      "lon": "-73.9654",                 // Map this to -> lng
//      "display_name": "Central Park, Manhattan, New York...", // -> name
//      "address": {                       // -> venueAddress
//    "leisure": "Central Park",
//        "road": "Central Park West",
//        "city": "New York",
//        "country": "USA"
//  }
//  }

  /**
   * This method helps the user import a Venue from ORM maps (Open source software) and use it
   *
   * The user will be ADMIN for new venues that are listed on the ORM maps.
   *
   */
  @Transactional
  public VenueResponse getOrCreatePublicVenue(String name, String address, Double lat, Double lng, String externalId) {

    validateInput(name, lat, lng, externalId);
    String normalizedName = name != null ? name.trim() : null;

   // ----- 1 . CHECK FOR DUPLICATES -------------

    // option A: Check to see if externalID = !null; if so then check for duplicates from the DB
    if(externalId != null) {
      // if it is a duplicate, I want to return the Venue from the DB
      Optional<Venue> exisitngVenue = venueRepository.findByExternalId(externalId);



      if(exisitngVenue.isPresent()) { return venueMapper.toResponse(exisitngVenue.get()); }
    }

    // using a second if to check for pin proximity to an existing venue in db.

    // Option B: If externalID = null, then compare the lat and long of the data
     if(lat != null && lng != null) {
      Optional<Venue> nearby = venueRepository.findFirstByLocationNear(lat, lng, MIN_SEARCH_RADIUS_M);
      if (nearby.isPresent()) {
        logger.info("Found existing venue within 50m: {}", nearby.get().getName());
        return venueMapper.toResponse(nearby.get());
      }
    }

//Option C: Check Fuzzy Name + Proximity (Prevent duplicates like "Rajesh Tennis" vs "Rajesh Court")
    if (normalizedName != null && !normalizedName.isBlank() && lat != null && lng != null) {
      Optional<Venue> nearbyWithSimilarName = findSimilarVenueNearby(
          normalizedName, lat, lng, MAX_SEARCH_RADIUS_M);

      if (nearbyWithSimilarName.isPresent()) {
        logger.info("Found similar venue within 200m: {}", nearbyWithSimilarName.get().getName());
        return venueMapper.toResponse(nearbyWithSimilarName.get());
      }
    }


    // ------------ 2. Fetch User and add fill in data ----------------

    // fetch the ADMIN User
    User systemOwner = userRepository.findByEmail(SYSTEM_ADMIN_EMAIL)
        .orElseThrow(() -> new RuntimeException("System Admin User missing! Run DB seeds."));

    // If the venue is not listed in DB, then create a Venue
    Venue venue = new Venue();
    venue.setName(name != null ? name : "Unknown Location");
    venue.setAddress(address != null ? address : "Custom Pin Drop");
    venue.setOwner(systemOwner); // this assigns the owner as ADMIN
    venue.setManaged(false); // Important: This prevents bookings
    venue.setActive(true);

    // If it has an ID, it came from an external map provider
    if (externalId != null) {

      venue.setSource(VenueSource.GOOGLE_PLACES); // or VenueSource.OSM
      venue.setExternalId(externalId);
    } else {
      // No ID = It's a raw user pin drop
      venue.setSource(VenueSource.AUTO_CREATED);
      venue.setExternalId("pin_" + lat + "_" + lng);
    }

    if (lat != null && lng != null) {
      venue.setLatitude(lat);
      venue.setLongitude(lng);
    }

    // Add Default values here
    venue.setPhoneNumber("N/A");
    venue.setDescription("Public venue imported from external source.");
    venue.setOpeningHours(java.util.Collections.emptyMap());
    venue.setAmenities(List.of("Public Access"));

    // --------------- Save the Venue in the DB and return

    try {
      // Attempt to save
      Venue savedVenue = venueRepository.save(venue);
      return venueMapper.toResponse(savedVenue);

    } catch (DataIntegrityViolationException e) {
      // RACE CONDITION DETECTED!
      // This block runs if someone else inserted the venue milliseconds before us.
      logger.info("Race condition hit for venue: {}. Fetching existing.", externalId);

      // 1. Determine the ID to search for
      String searchId = (externalId != null) ? externalId : "pin_" + lat + "_" + lng;

      // 2. Fetch the one that "won" the race
      Venue winner = venueRepository.findByExternalId(searchId)
          .orElseThrow(() -> new RuntimeException("Concurrency Error: Venue exists but cannot be found."));

      return venueMapper.toResponse(winner);
    }
  }


  // ----------------HELPER METHODS ------------------
  private void validateInput(String name, Double lat, Double lng, String externalId) {
    // Must have EITHER externalId OR coordinates
    if ((externalId == null || externalId.isBlank()) && (lat == null || lng == null)) {
      throw new IllegalArgumentException(
          "Must provide either externalId OR both latitude and longitude"
      );
    }

    // Validate coordinates if provided
    if (lat != null || lng != null) {
      if (lat == null || lng == null) {
        throw new IllegalArgumentException("Both latitude and longitude are required");
      }

      if (lat < -90 || lat > 90) {
        throw new IllegalArgumentException("Latitude must be between -90 and 90");
      }

      if (lng < -180 || lng > 180) {
        throw new IllegalArgumentException("Longitude must be between -180 and 180");
      }
    }

    // Validate name length
    if (name != null && name.length() > 200) {
      throw new IllegalArgumentException("Venue name too long (max 200 characters)");
    }
  }




  private Optional<Venue> findSimilarVenueNearby(String normalizedName, Double lat, Double lng, double radiusMeters) {

    // 1. Get venues nearby using PostGIS (Fast)
    // We use Pageable to limit to 20 results max to be safe
    Page<Venue> nearbyVenues = venueRepository.findVenuesNearby(lat, lng, radiusMeters, PageRequest.of(0, 20));

    // 2. Filter by Name Similarity (Levenshtein or Contains)
    for (Venue venue : nearbyVenues.getContent()) {
      if (isNameSimilar(normalizedName, venue.getName())) {
        return Optional.of(venue);
      }
    }

    return Optional.empty();
  }

  private boolean isNameSimilar(String inputName, String dbName) {
    if (dbName == null) return false;
    String s1 = inputName.toLowerCase();
    String s2 = dbName.toLowerCase();

    // Exact substring match (e.g. "Rajesh Tennis" contains "Rajesh")
    return s1.contains(s2) || s2.contains(s1);
  }

}
