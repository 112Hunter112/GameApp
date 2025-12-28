package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Venue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface VenueRepository extends CrudRepository<Venue, Long> {

  @Query(value = "SELECT * FROM venues v WHERE " +
      "ST_DWithin(v.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), :radiusInMeters)",
      nativeQuery = true)
  List<Venue> findVenuesNearby(double lat, double lng, double radiusInMeters);

}
