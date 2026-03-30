package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

  @Query(value = """
        SELECT * FROM venues v
        WHERE v.is_active = true
        AND (6371000 * acos(LEAST(1.0,
            cos(radians(:lat)) * cos(radians(v.latitude)) *
            cos(radians(v.longitude) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(v.latitude))
        ))) <= :radius
        """,
      countQuery = """
        SELECT count(*) FROM venues v
        WHERE v.is_active = true
        AND (6371000 * acos(LEAST(1.0,
            cos(radians(:lat)) * cos(radians(v.latitude)) *
            cos(radians(v.longitude) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(v.latitude))
        ))) <= :radius
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



  // checks to see if a venue exists in a certain radius
  @Query(value = """
        SELECT * FROM venues
        WHERE is_managed = false
        AND (6371000 * acos(LEAST(1.0,
            cos(radians(:latitude)) * cos(radians(latitude)) *
            cos(radians(longitude) - radians(:longitude)) +
            sin(radians(:latitude)) * sin(radians(latitude))
        ))) <= :radiusMeters
        ORDER BY (6371000 * acos(LEAST(1.0,
            cos(radians(:latitude)) * cos(radians(latitude)) *
            cos(radians(longitude) - radians(:longitude)) +
            sin(radians(:latitude)) * sin(radians(latitude))
        )))
        LIMIT 1
        """, nativeQuery = true)
  Optional<Venue> findNearbyUnmanagedVenue(
      @Param("latitude") Double latitude,
      @Param("longitude") Double longitude,
      @Param("radiusMeters") Double radiusMeters
  );

  @Query(value = """
    SELECT * FROM venues v
    WHERE (6371000 * acos(LEAST(1.0,
        cos(radians(:lat)) * cos(radians(v.latitude)) *
        cos(radians(v.longitude) - radians(:lng)) +
        sin(radians(:lat)) * sin(radians(v.latitude))
    ))) <= :radiusMeters
    LIMIT 1
""", nativeQuery = true)
  Optional<Venue> findFirstByLocationNear(
      @Param("lat") double lat,
      @Param("lng") double lng,
      @Param("radiusMeters") double radiusMeters
  );

  boolean existsByExternalId(String externalId);

}
