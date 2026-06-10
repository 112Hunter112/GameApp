package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Courts;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourtRepository extends JpaRepository<Courts, UUID> {

  /**
   * Lock the court row (SELECT ... FOR UPDATE) for the duration of the transaction.
   * BookingService takes this lock before checking for conflicting bookings so that
   * two players hitting "Book" at the same instant cannot both pass the check.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Courts c WHERE c.id = :id")
  Optional<Courts> findByIdForUpdate(@Param("id") UUID id);


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
