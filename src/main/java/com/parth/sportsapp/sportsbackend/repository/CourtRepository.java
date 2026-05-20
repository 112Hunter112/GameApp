package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Courts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CourtRepository extends JpaRepository<Courts, UUID> {


  //  ---- Find all possible courts at the venue


  List<Courts> findByVenue_IdAndSports_Id(UUID venueId, UUID sportId);

  // ------ all filters

  // filter for sports name
  List<Courts> findBySports_SportNameContainingIgnoreCase(String sportName);

  List<Courts> findByIsActiveTrue();


  List<Courts> findByVenue_Id(UUID Id);


  List<Courts> findByIsIndoorTrue();


  List<Courts> findByHourlyRateLessThanEqual(BigDecimal maxPrice);

  List<Courts> findBySurfaceTypeIgnoreCase(String surfaceType);

  List<Courts> findByVenue_IdIn(List<UUID> venueIds);

  List<Courts> findBySports_Id(UUID sportId);



  // ----- count of all the items

  // tells users how many courts there are for the specific sport
  int countBySports_SportName(String sportsSportName);

  // helpful to list out all courts at a venue to help people
  int countByVenue_Id(UUID venueId);
}
