package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.CourtRequest;
import com.parth.sportsapp.sportsbackend.dto.CourtResponse;
import com.parth.sportsapp.sportsbackend.dto.VenueRequest;
import com.parth.sportsapp.sportsbackend.mapper.CourtMapper;
import com.parth.sportsapp.sportsbackend.model.Courts;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.repository.CourtRepository;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import com.parth.sportsapp.sportsbackend.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourtService {

  @Autowired
  private CourtRepository courtRepository;

  @Autowired
  private VenueRepository venueRepository;

  @Autowired
  private SportsRepository sportsRepository; // <--- You need this to verify the sport

  @Autowired
  private CourtMapper courtMapper;


  //--------VENDOR-----------

  public CourtResponse addCourt(UUID venueId, UUID vendorId, CourtRequest request) {

    // 1. We verify if the venue actually exists in the DB or just guessing
    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new RuntimeException("Venue not found"));

    //2. Verify if the vendor ID matches the venueID.owner for which the Court is being added for
    if (!venue.getOwner().getId().equals(vendorId)) {
      throw new RuntimeException("Not Authorized");
    }

    //3. Verify if the sport chosen is correct
    Sports sport = sportsRepository.findById(request.getSportId())
        .orElseThrow(() -> new RuntimeException("Sport not found"));

    validateCourtData(request);

    // 4. Convert Request -> Entity (Prepare for DB)
    // The mapper puts the relationships (venue/sport) into the court
    Courts courtEntity = courtMapper.toEntity(request, venue, sport);

    // 5. Save to DB (This generates the ID!)
    Courts savedCourt = courtRepository.save(courtEntity);

    // 5. Convert Entity -> Response (Send back the new ID)
    return courtMapper.toResponse(savedCourt);


  }


  public CourtResponse updateCourt(UUID courtId, UUID vendorId, CourtRequest request) {

    Courts court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    // SECURITY CHECK: Trace back to the owner
    // Court -> Venue -> Owner
    if (!court.getVenue().getOwner().getId().equals(vendorId)) {
      throw new RuntimeException("ACCESS DENIED: You do not own this court");
    }

    validateCourtData(request);

    // Update fields (We usually don't allow changing Sport or Venue on update)
    court.setCourtNumber(request.getCourtNumber());
    court.setHourlyRate(request.getHourlyRate());
    court.setIndoor(request.getIsIndoor());
    court.setSurfaceType(request.getSurfaceType());
    court.setCapacity(request.getCapacity());
    court.setAmenities(request.getAmenities());

    Courts updatedCourt = courtRepository.save(court);
    return courtMapper.toResponse(updatedCourt);
  }


  // 3. DELETE COURT (Vendor Only)
  public void deleteCourt(UUID courtId, UUID vendorId) {
    Courts court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));

    if (!court.getVenue().getOwner().getId().equals(vendorId)) {
      throw new RuntimeException("ACCESS DENIED");
    }

    // Soft delete - just mark as inactive
    court.setActive(false);
    courtRepository.save(court);
  }

  /**
   * Get all courts owned by vendor (across all their venues)
   */
  @Transactional(readOnly = true)
  public List<CourtResponse> getVendorCourts(UUID vendorId) {
    // Get all vendor's venues
    List<Venue> venues = venueRepository.findByOwner_Id(vendorId);

    // Get venue IDs
    List<UUID> venueIds = venues.stream()
        .map(Venue::getId)
        .collect(Collectors.toList());

    // Get all courts for these venues
    List<Courts> courts = courtRepository.findByVenue_IdIn(venueIds);

    // Convert to DTOs
    return courts.stream()
        .map(courtMapper::toResponse)
        .collect(Collectors.toList());
  }

  // ----------------------USER---------------------



  //  GET ALL COURTS FOR A VENUE (Public - Users need this!)
  public List<CourtResponse> getCourtsByVenue(UUID venueId) {
    List<Courts> courts = courtRepository.findByVenue_Id(venueId);

    // Convert List<Entity> -> List<DTO>
    return courts.stream()
        .map(court -> courtMapper.toResponse(court))
        .collect(Collectors.toList());
  }



  //  GET SINGLE COURT (Public)
  public CourtResponse getCourtById(UUID courtId) {
    Courts court = courtRepository.findById(courtId)
        .orElseThrow(() -> new RuntimeException("Court not found"));
    return courtMapper.toResponse(court);
  }



  /**
   * Get all courts for a specific sport
   */
  @Transactional(readOnly = true)
  public List<CourtResponse> getCourtsBySport(UUID sportId) {
    List<Courts> courts = courtRepository.findBySport_Id(sportId);
    return courts.stream()
        .map(courtMapper::toResponse)
        .collect(Collectors.toList());
  }

  public List<CourtResponse> getCourtsByVenueAndSport(UUID venueId, UUID sportId) {
    List<Courts> courts = courtRepository.findByVenue_IdAndSports_Id(venueId, sportId);
    return courts.stream().map(courtMapper::toResponse).collect(Collectors.toList());
  }


//  /**
//   * Check if court is available at given time
//   */
//  @Transactional(readOnly = true)
//  public boolean isCourtAvailable(UUID courtId, LocalDateTime startTime, LocalDateTime endTime) {
//    // Check if court exists and is active
//    Courts court = courtRepository.findById(courtId)
//        .orElseThrow(() -> new RuntimeException("Court not found"));
//
//    if (!court.isActive()) {
//      return false;
//    }
//
//    // Check for conflicting bookings
//    Long conflicts = bookingRepository.countConflictingBookings(courtId, startTime, endTime);
//
//    return conflicts == 0;
//  }





  private void validateCourtData(CourtRequest request) {
    if (request.getHourlyRate() != null && request.getHourlyRate().compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Hourly rate cannot be negative");
    }

    if (request.getCapacity() != null && request.getCapacity() < 1) {
      throw new IllegalArgumentException("Capacity must be at least 1");
    }

    if (request.getCourtNumber() == null || request.getCourtNumber().trim().isEmpty()) {
      throw new IllegalArgumentException("Court number is required");
    }
  }

}
