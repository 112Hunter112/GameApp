package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Sports;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SportsRepository extends JpaRepository<Sports, UUID> {


  Optional<Sports> findBySportName(String sportName);
  Optional<Sports> findBySportNameIgnoreCase(String sportName);

  int countBySportName(String sportName); // counts all the possible sports available

  boolean existsBySportNameIgnoreCase(String sportName);
  long countByIsActiveTrue(); // all isActive when true
  long countByIsActiveFalse();

  List<Sports> findByIsActiveTrue();
  List<Sports> findByMinPlayersGreaterThanEqual(int min);
  List<Sports> findByMaxPlayersLessThanEqual(int max);
  List<Sports> findByMinPlayersBetween(int min, int max);

  @Query("SELECT s FROM Sports s WHERE LOWER(s.sportName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
  List<Sports> searchByName(@Param("keyword") String keyword);

  @Query(value = "SELECT * FROM sports WHERE is_active = true ORDER BY sport_name", nativeQuery = true)
  List<Sports> getActiveSportsNative();

  Page<Sports> findByIsActiveTrue(Pageable pageable);

  List<Sports> findBySportNameContainingIgnoreCase(String keyword);

  List<Sports> findByMinPlayersGreaterThanEqualOrMaxPlayersLessThanEqual(int min, int max);

  /**
   * Active sports that have at least one court hosted by a venue within the
   * given radius of (lat, lng). Used to power the home-screen "Discover sports
   * near you" strip — anything in this list is currently playable nearby but
   * the user hasn't necessarily added to their preferences.
   */
  @Query(value = """
      SELECT DISTINCT s.id, s.sport_name, s.icon_url
      FROM sports s
      JOIN courts c   ON c.sports_id = s.id
      JOIN venues v   ON v.id = c.venue_id
      WHERE s.is_active = true
        AND v.is_active = true
        AND v.location IS NOT NULL
        AND ST_DWithin(
            v.location::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
            :radius
        )
      ORDER BY s.sport_name
      """, nativeQuery = true)
  List<Object[]> findActiveSportsNearby(
      @Param("lat") double lat,
      @Param("lng") double lng,
      @Param("radius") double radius);
}

