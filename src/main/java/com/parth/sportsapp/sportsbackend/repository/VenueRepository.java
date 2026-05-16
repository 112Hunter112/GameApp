package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

  @Query(value = """
        SELECT * FROM venues v
        WHERE v.is_active = true
          AND v.location IS NOT NULL
          AND ST_DWithin(
              v.location::geography,
              ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
              :radius
          )
        ORDER BY ST_Distance(
              v.location::geography,
              ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
          ) ASC, v.id ASC
        """,
      countQuery = """
        SELECT count(*) FROM venues v
        WHERE v.is_active = true
          AND v.location IS NOT NULL
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

  /**
   * Find venues with similar names (fuzzy match)
   */
  @Query("SELECT v FROM Venue v WHERE " +
      "LOWER(TRIM(v.name)) LIKE LOWER(CONCAT('%', :name, '%')) " +
      "AND v.isManaged = false " +
      "ORDER BY v.createdAt DESC")
  List<Venue> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

  List<Venue> findByOwner_FirstNameIgnoreCase(String firstName);

  List<Venue> findByAmenitiesIn(List<String> amenities);

List<Venue> findByAddressContainingIgnoreCase(String address);

  Page<Venue> findAll(Pageable pageable);

  List<Venue> findAllByOrderByCreatedAtDesc();

  // returns a list * of all Venues with Owner_id
  List<Venue> findByOwner_Id(UUID id);

  //
  boolean existsByNameIgnoreCaseAndAddressIgnoreCase(String name, String address);

  // src/main/java/com/parth/sportsapp/sportsbackend/repository/VenueRepository.java

  @Query("SELECT DISTINCT v FROM Venue v JOIN v.courts c JOIN c.sports s WHERE LOWER(s.sportName) LIKE LOWER(CONCAT('%', :sportName, '%'))")
  List<Venue> findBySportName(@Param("sportName") String sportName);

  @Query ("SELECT DISTINCT v FROM Venue v WHERE v.isManaged = true ")
  Page<Venue>  findVenueByIsManagedIsTrue(@Param("isManaged") boolean isManaged, Pageable pageable);


  // this finds all the venues that are not yet registered with us
  Optional<Venue> findByExternalId(String externalId);


  @Query("SELECT DISTINCT v FROM Venue v WHERE v.isManaged = :isManaged")
  Page<Venue> findVenueByIsManaged(@Param("isManaged") boolean isManaged, Pageable pageable);



  // checks to see if an unmanaged venue exists within a given radius (meters) of the given point
  @Query(value = """
        SELECT * FROM venues v
        WHERE v.is_managed = false
          AND v.location IS NOT NULL
          AND ST_DWithin(
              v.location::geography,
              ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
              :radiusMeters
          )
        ORDER BY ST_Distance(
              v.location::geography,
              ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
          ) ASC
        LIMIT 1
        """, nativeQuery = true)
  Optional<Venue> findNearbyUnmanagedVenue(
      @Param("latitude") Double latitude,
      @Param("longitude") Double longitude,
      @Param("radiusMeters") Double radiusMeters
  );

  @Query(value = """
        SELECT * FROM venues v
        WHERE v.location IS NOT NULL
          AND ST_DWithin(
              v.location::geography,
              ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
              :radiusMeters
          )
        ORDER BY ST_Distance(
              v.location::geography,
              ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
          ) ASC
        LIMIT 1
        """, nativeQuery = true)
  Optional<Venue> findFirstByLocationNear(
      @Param("lat") double lat,
      @Param("lng") double lng,
      @Param("radiusMeters") double radiusMeters
  );

  boolean existsByExternalId(String externalId);

  // TODO : A query that finds the distance adn returns data in VenueDistanceProjection format
  //select id, name and distance. From venue, where distance  is less than equal to user specified
  // range from user start, location exists
  @Query(value = """
    SELECT v.id AS id,
           v.name AS name,
           ST_Distance(
               v.location::geography,
               ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
           ) AS distance_meters
    FROM venues v
    WHERE v.is_active = true
      AND v.location IS NOT NULL
      AND ST_DWithin(
          v.location::geography,
          ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
          :radiusMeters
      )
    ORDER BY distance_meters ASC, v.id ASC
    """,
      countQuery = """
    SELECT COUNT(*)
    FROM venues v
    WHERE v.is_active = true
      AND v.location IS NOT NULL
      AND ST_DWithin(
          v.location::geography,
          ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
          :radiusMeters
      )
    """,
      nativeQuery = true)
  Page<VenueDistanceProjection> findNearbyWithDistance(
      @Param("latitude")     double latitude,
      @Param("longitude")    double longitude,
      @Param("radiusMeters") double radiusMeters,
      Pageable pageable
  );

}
