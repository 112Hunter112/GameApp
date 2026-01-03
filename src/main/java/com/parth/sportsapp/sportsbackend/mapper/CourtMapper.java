package com.parth.sportsapp.sportsbackend.mapper;


import com.parth.sportsapp.sportsbackend.dto.CourtRequest;
import com.parth.sportsapp.sportsbackend.dto.CourtResponse;
import com.parth.sportsapp.sportsbackend.model.Courts;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.model.Venue;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.Mapping;

@Component
public class CourtMapper {

// convert court request to court Entity
public Courts toEntity(CourtRequest request, Venue venue, Sports sport) {
  if (request == null) {
    return null;
  }

  Courts court = new Courts();

  // 1. Map simple fields
  court.setCourtNumber(request.getCourtNumber());
  court.setHourlyRate(request.getHourlyRate());
  court.setIndoor(request.getIsIndoor());
  court.setSurfaceType(request.getSurfaceType());
  court.setCapacity(request.getCapacity());
  court.setAmenities(request.getAmenities()); // Assuming your Entity has this list

  // 2. Map Relationships
  court.setVenue(venue);
  court.setSports(sport);

  return court;

}

  public CourtResponse toResponse(Courts court) {
    if (court == null) return null;

    CourtResponse response = new CourtResponse();

    // Map IDs
    response.setId(court.getId());
    response.setVenueId(court.getVenue().getId());

    // Map Sport Details (Frontend needs the name, not just ID!)
    response.setSportId(court.getSports().getId());
    response.setSportName(court.getSports().getSportName());

    // Map Simple Fields
    response.setCourtNumber(court.getCourtNumber());
    response.setHourlyRate(court.getHourlyRate());
    response.setIndoor(court.isIndoor());
    response.setSurfaceType(court.getSurfaceType());
    response.setCapacity(court.getCapacity());
    response.setAmenities(court.getAmenities());

    return response;
  }

}
