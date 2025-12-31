package com.parth.sportsapp.sportsbackend.service;


import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import com.parth.sportsapp.sportsbackend.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;



/*
"My Venues" Page: You return a list of Venues (Names & Addresses only).

Vendor sees: "Downtown Arena", "Uptown Gym".

The Click: The Vendor clicks on "Downtown Arena" (ID: 100).

The Request: Frontend calls GET /api/venues/100/courts.

The Service: Calls courtRepository.findByVenueId(100).

The Result: Returns [Court 1, Court 2].
 */




@Service  @Transactional

public class VenueService {

  // Add this library (JTS) to handle the shapes
  private final org.locationtech.jts.geom.GeometryFactory geometryFactory = new org.locationtech.jts.geom.GeometryFactory();

  @Autowired
  private VenueRepository venueRepository;

@Autowired
private UserRepository userRepository;

  private static final double KM_TO_METERS = 1000.0;



  // ----------------VENDOR SIDE ------------------

  //list all venues for the vendor after verifying that it is a VENDOR
  public List<Venue> findVenueByOwnerId(UUID vendorId) {

    // checks to see if the vendor exists, this implicitly chekcs if person is a VENDOR
    User vendor = userRepository.findById(vendorId)
        .orElseThrow(() -> new RuntimeException("Vendor not found"));

    return venueRepository.findByOwner_Id(vendor.getId());
  }

  /**
   *
   * @param vendorId the UUID the of the user from the jwt
   * @param venue the data the user entered and then pressed save
   * @return a saved Venue of the data the user entered on the frontend
   */
  public Venue createVenue(UUID vendorId, Venue venue) {
    // 0. Opens a list to fill out which the user does and when they press save we call this method

    // 0.1 Checks that this person passing it through has a valid ID and in VENDOR owner list
    User vendor = userRepository.findById(vendorId)
        .orElseThrow(() -> new RuntimeException("Vendor not found"));

    // 0.2 FORCE the ownership here
    venue.setOwner(vendor);

    if (venue.getLatitude() != null && venue.getLongitude() != null) {
      org.locationtech.jts.geom.Point point = geometryFactory.createPoint(
          new org.locationtech.jts.geom.Coordinate(venue.getLongitude(), venue.getLatitude())
      );
      venue.setLocation(point);
    }

    // 1. Unique name, address, phone number, etc.
    validateVenueInfo(venue);

    // 2. Check for Duplicates
    if (venue.getId() == null) { // Only check duplicates if it's a NEW venue
      boolean exists = venueRepository.existsByNameIgnoreCaseAndAddressIgnoreCase(
          venue.getName(),
          venue.getAddress()
      );

      if (exists) {
        throw new RuntimeException("A venue with this name and address already exists!");
      }
    }

    return venueRepository.save(venue);
  }

  /**
   * once a user clicks on the venue they get a venueId for the venue they clicked on
   *
   *
   * @param venueId this is the ID of the box the user clicked on
   * @param vendorId this comes from the UUID
   * @param update_venue
   * @return
   */
  public Venue updateVenue(UUID venueId,  UUID vendorId, Venue update_venue) {

 // 0.1 check if venue exists using venueId, before we update, has not been compared to
    // vendors' current active venue
    Venue existingVenue = venueRepository.findById(venueId)
        .orElseThrow(() -> new RuntimeException("Venue not found with ID: " + venueId));

    //0.2 Check if the vendor ID is the same as venueID's owner we want to update
    if (!existingVenue.getOwner().getId().equals(vendorId)) {
      throw new RuntimeException("ACCESS DENIED: You do not own this venue.");
    }

    if (update_venue.getLatitude() != null && update_venue.getLongitude() != null) {
      org.locationtech.jts.geom.Point point = geometryFactory.createPoint(
          new org.locationtech.jts.geom.Coordinate(update_venue.getLongitude(), update_venue.getLatitude())
      );
      update_venue.setLocation(point);
    }


    //2. validate the new info the user has entered
    validateVenueInfo(update_venue);

    existingVenue.setName(update_venue.getName());
    existingVenue.setAddress(update_venue.getAddress());
    existingVenue.setPhoneNumber(update_venue.getPhoneNumber());
    existingVenue.setDescription(update_venue.getDescription());
    existingVenue.setOpeningHours(update_venue.getOpeningHours());
    existingVenue.setAmenities(update_venue.getAmenities());
    if (update_venue.getLocation() != null) {
      existingVenue.setLocation(update_venue.getLocation());
    }

    return venueRepository.save(existingVenue); // save old venue with new content
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


  // ----------------USER SIDE ------------------

  public List<Venue> searchNearbyVenues(double lat, double lon, double radiusKm) {
    // 1. Convert KM to Meters (PostGIS geography uses meters)
    double radiusMeters = radiusKm * KM_TO_METERS;

    // 2. Call the repository
    return venueRepository.findNearby(lat, lon, radiusMeters);
  }


}
