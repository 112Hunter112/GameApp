package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
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
   * Unified discover/search: free-text over name+address, optional sport filter,
   * optional location for distance sorting — all in one query.
   *
   * Optional params arrive as STRINGS where '' means "not provided"; NULLIF turns
   * them into SQL NULLs so casts are safe regardless of evaluation order (a plain
   * ':p = '' OR CAST(:p AS uuid)' can still cast-fail in Postgres, which does not
   * guarantee OR short-circuiting).
   */
  @Query(value = """
      SELECT v.id AS id, v.name AS name, v.address AS address,
             CASE WHEN NULLIF(:lat, '') IS NULL THEN NULL
                  ELSE ST_Distance(
                      v.location::geography,
                      ST_SetSRID(ST_MakePoint(
                          CAST(NULLIF(:lng, '') AS float8),
                          CAST(NULLIF(:lat, '') AS float8)), 4326)::geography)
             END AS "distanceMeters"
      FROM venues v
      WHERE v.is_active = true
        AND (:q = '' OR v.name ILIKE '%' || :q || '%' OR v.address ILIKE '%' || :q || '%')
        AND (NULLIF(:sportId, '') IS NULL OR EXISTS (
              SELECT 1 FROM courts c
              WHERE c.venue_id = v.id
                AND c.is_active = true
                AND c.sports_id = CAST(NULLIF(:sportId, '') AS uuid)))
      ORDER BY CASE WHEN NULLIF(:lat, '') IS NULL THEN 0
                    ELSE ST_Distance(
                        v.location::geography,
                        ST_SetSRID(ST_MakePoint(
                            CAST(NULLIF(:lng, '') AS float8),
                            CAST(NULLIF(:lat, '') AS float8)), 4326)::geography)
               END ASC NULLS LAST,
               v.name ASC
      """,
      countQuery = """
      SELECT count(*)
      FROM venues v
      WHERE v.is_active = true
        AND (:q = '' OR v.name ILIKE '%' || :q || '%' OR v.address ILIKE '%' || :q || '%')
        AND (NULLIF(:sportId, '') IS NULL OR EXISTS (
              SELECT 1 FROM courts c
              WHERE c.venue_id = v.id
                AND c.is_active = true
                AND c.sports_id = CAST(NULLIF(:sportId, '') AS uuid)))
      """,
      nativeQuery = true)
  Page<VenueSearchProjection> discoverVenues(
      @Param("q") String q,
      @Param("sportId") String sportId,
      @Param("lat") String lat,
      @Param("lng") String lng,
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

  /**
   * Paginated newest-first listing. The page size cap configured globally in
   * application.yml (spring.data.web.pageable.max-page-size) keeps a malicious
   * ?size=10000000 from OOM-ing the JVM.
   */
  Page<Venue> findAllByOrderByCreatedAtDesc(Pageable pageable);

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

  /**
   * Personalized nearby search: only venues that host AT LEAST ONE court for
   * one of the supplied sports. Used by the home-feed endpoint to filter venues
   * down to the user's preferred sports.
   */
  @Query(value = """
      SELECT v.id   AS id,
             v.name AS name,
             ST_Distance(
                 v.location::geography,
                 ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
             ) AS distance_meters
      FROM venues v
      JOIN courts c ON c.venue_id = v.id
      WHERE v.is_active = true
        AND v.location IS NOT NULL
        AND c.sports_id IN (:sportIds)
        AND ST_DWithin(
            v.location::geography,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
            :radiusMeters
        )
      GROUP BY v.id, v.name, v.location
      ORDER BY distance_meters ASC, v.id ASC
      """,
      countQuery = """
      SELECT COUNT(DISTINCT v.id)
      FROM venues v
      JOIN courts c ON c.venue_id = v.id
      WHERE v.is_active = true
        AND v.location IS NOT NULL
        AND c.sports_id IN (:sportIds)
        AND ST_DWithin(
            v.location::geography,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
            :radiusMeters
        )
      """,
      nativeQuery = true)
  Page<VenueDistanceProjection> findNearbyForSports(
      @Param("latitude")     double latitude,
      @Param("longitude")    double longitude,
      @Param("radiusMeters") double radiusMeters,
      @Param("sportIds")     Collection<UUID> sportIds,
      Pageable pageable
  );

}
