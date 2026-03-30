package com.parth.sportsapp.sportsbackend.mapper;

import com.parth.sportsapp.sportsbackend.dto.VenueRequest;
import com.parth.sportsapp.sportsbackend.dto.VenueResponse;
import com.parth.sportsapp.sportsbackend.dto.VenueResponse.OwnerSummaryDto;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VenueMapper {

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

    venue.setLatitude(request.getLatitude());
    venue.setLongitude(request.getLongitude());

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

    if (request.getLatitude() != null && request.getLongitude() != null) {
      venue.setLatitude(request.getLatitude());
      venue.setLongitude(request.getLongitude());
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

    response.setLatitude(venue.getLatitude());
    response.setLongitude(venue.getLongitude());

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
