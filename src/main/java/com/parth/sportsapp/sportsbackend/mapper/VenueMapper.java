package com.parth.sportsapp.sportsbackend.mapper;

import com.parth.sportsapp.sportsbackend.dto.VenueRequest;
import com.parth.sportsapp.sportsbackend.dto.VenueResponse;
import com.parth.sportsapp.sportsbackend.dto.VenueResponse.OwnerSummaryDto;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VenueMapper {

  // PostGIS requires SRID 4326 (WGS84 coordinate system)
  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

  // ========== REQUEST DTO → ENTITY ==========

  /**
   * Convert VenueRequest to Venue entity (for CREATE)
   */
  public Venue toEntity(VenueRequest request, User owner) {

    if (request == null) {
      return null;
    }

    Venue venue = new Venue();

    // Basic fields
    venue.setName(request.getName());
    venue.setAddress(request.getAddress());
    venue.setPhoneNumber(request.getPhoneNumber());
    venue.setDescription(request.getDescription());
    venue.setOpeningHours(request.getOpeningHours());
    venue.setAmenities(request.getAmenities() != null ? request.getAmenities() : new ArrayList<>());



    // Set owner (FORCE ownership - security!)
    venue.setOwner(owner);

    // Convert lat/lng to PostGIS Point
    Point point = geometryFactory.createPoint(
        new Coordinate(request.getLongitude(), request.getLatitude())
    );
    venue.setLocation(point);

    // New venues are active by default
    venue.setActive(true);

    return venue;
  }

  /**
   * Update existing Venue entity from VenueRequest (for UPDATE)
   */
  public void updateEntity(Venue venue, VenueRequest request) {
    // Update basic fields
    venue.setName(request.getName());
    venue.setAddress(request.getAddress());
    venue.setPhoneNumber(request.getPhoneNumber());
    venue.setDescription(request.getDescription());
    venue.setOpeningHours(request.getOpeningHours());
    venue.setAmenities(request.getAmenities() != null ? request.getAmenities() : new ArrayList<>());

    // Update location if coordinates provided
    if (request.getLatitude() != null && request.getLongitude() != null) {
      Point point = geometryFactory.createPoint(
          new Coordinate(request.getLongitude(), request.getLatitude())
      );
      venue.setLocation(point);
    }

    // NOTE: We DON'T update owner or isActive here - those are controlled separately
  }

  // ========== ENTITY → RESPONSE DTO ==========

  /**
   * Convert Venue entity to VenueResponse DTO
   */
  public VenueResponse toResponse(Venue venue) {
    VenueResponse response = new VenueResponse();

    // Basic fields
    response.setId(venue.getId());
    response.setName(venue.getName());
    response.setAddress(venue.getAddress());
    response.setPhoneNumber(venue.getPhoneNumber());
    response.setDescription(venue.getDescription());
    response.setOpeningHours(venue.getOpeningHours());
    response.setActive(venue.isActive());

    response.setExternalId(venue.getExternalId());
    response.setSource(venue.getSource());

    // ====================================================================
    // FIX: Copy the list to a new ArrayList to force it to load immediately
    // ====================================================================
    List<String> safeAmenities = venue.getAmenities() != null
        ? new ArrayList<>(venue.getAmenities())
        : new ArrayList<>();
    response.setAmenities(safeAmenities);
    // ====================================================================

    // Extract coordinates from PostGIS Point
    if (venue.getLocation() != null) {
      response.setLatitude(venue.getLocation().getY());   // Y = Latitude
      response.setLongitude(venue.getLocation().getX());  // X = Longitude
    }

    // Convert owner to safe DTO
    if (venue.getOwner() != null) {
      User owner = venue.getOwner();
      OwnerSummaryDto ownerDto = new OwnerSummaryDto(
          owner.getId(),
          owner.getFirstName(),
          owner.getLastName(),
          owner.getEmail()
      );
      response.setOwner(ownerDto);
    }

    return response;
  }

  /**
   * Convert list of Venue entities to list of VenueResponse DTOs
   */
  public List<VenueResponse> toResponseList(List<Venue> venues) {
    return venues.stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }
}
