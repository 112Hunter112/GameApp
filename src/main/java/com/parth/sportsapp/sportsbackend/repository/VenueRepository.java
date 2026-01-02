package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

  @Query(value = """
        SELECT * FROM venues v
        WHERE v.is_active = true 
        AND ST_DWithin(
            v.location::geography,  -- Cast column to geography for meter calculation
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, 
            :radius
        )
        """,
      countQuery = """
        SELECT count(*) FROM venues v 
        WHERE v.is_active = true 
        AND ST_DWithin(
            v.location::geography, 
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, 
            :radius
        )
        """,
      nativeQuery = true)
  Page<Venue> findVenuesNearby(
      @Param("lat") double lat,
      @Param("lng") double lng,
      @Param("radius") double radius,
      Pageable pageable
  );

  List<Venue> findByNameContainingIgnoreCase(String name);

  List<Venue> findByOwner_FirstNameIgnoreCase(String firstName);

  List<Venue> findByAmenitiesIn(List<String> amenities);

List<Venue> findByAddressContainingIgnoreCase(String address);

  Page<Venue> findAll(Pageable pageable);

  List<Venue> findAllByOrderByCreatedAtDesc();

  // returns a list * of all Venues with Owner_id
  List<Venue> findByOwner_Id(UUID id);

  // inside VenueRepository interface
  boolean existsByNameIgnoreCaseAndAddressIgnoreCase(String name, String address);


}
