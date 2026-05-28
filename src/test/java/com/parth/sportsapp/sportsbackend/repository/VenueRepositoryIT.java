package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.ScoringType;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserRole;
import com.parth.sportsapp.sportsbackend.model.Venue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Real PostGIS integration test using Testcontainers. Spins up the actual
 * postgis/postgis Docker image so {@code ST_DWithin}, {@code ST_Distance},
 * and the geometry column behave the same way they will in production.
 *
 * <p>Requires Docker Desktop (or any Docker daemon) to be running on the
 * host before the test is invoked.</p>
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
class VenueRepositoryIT {

  @Container
  static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
      DockerImageName.parse("postgis/postgis:15-3.4")
          .asCompatibleSubstituteFor("postgres"))
      .withInitScript("init-postgis.sql");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGIS::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGIS::getUsername);
    registry.add("spring.datasource.password", POSTGIS::getPassword);
  }

  private static final GeometryFactory GF =
      new GeometryFactory(new PrecisionModel(), 4326);

  @Autowired private VenueRepository venueRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private SportsRepository sportsRepository;
  @PersistenceContext private EntityManager em;

  private User owner;

  @BeforeEach
  void setUp() {
    venueRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();

    owner = new User();
    owner.setFirstName("Owner");
    owner.setLastName("Test");
    owner.setEmail("owner-" + System.nanoTime() + "@example.com");
    owner.setPassword("placeholder");
    owner.setRole(UserRole.USER);
    owner.setJoiningDate(LocalDateTime.now());
    owner.setUpdatedAt(LocalDateTime.now());
    owner.setVerified(true);
    owner = userRepository.save(owner);
  }

  @Test
  void findNearbyWithDistanceReturnsResultsOrderedByDistance() {
    // Reference point: Chicago Loop
    double lat = 41.8781;
    double lng = -87.6298;

    // Three venues at known offsets in longitude — pure east/west so latitude
    // stays constant and the distance math is easy to reason about.
    saveVenue("Close",  lat, lng + 0.01);  // ~830 m east
    saveVenue("Medium", lat, lng + 0.05);  // ~4.1 km east
    saveVenue("Far",    lat, lng + 0.10);  // ~8.3 km east

    Page<VenueDistanceProjection> page = venueRepository.findNearbyWithDistance(
        lat, lng, 15_000, PageRequest.of(0, 10));

    assertThat(page.getTotalElements()).isEqualTo(3);
    assertThat(page.getContent())
        .extracting(VenueDistanceProjection::getName)
        .containsExactly("Close", "Medium", "Far");

    // Distances should be strictly ascending and roughly match expectations.
    Double d1 = page.getContent().get(0).getDistanceMeters();
    Double d2 = page.getContent().get(1).getDistanceMeters();
    Double d3 = page.getContent().get(2).getDistanceMeters();
    assertThat(d1).isLessThan(d2);
    assertThat(d2).isLessThan(d3);

    // Approximate: 0.01 degrees of longitude at 41.87 N ≈ 830 m.
    assertThat(d1).isBetween(700.0, 1000.0);
    assertThat(d3).isBetween(7500.0, 9000.0);
  }

  @Test
  void findNearbyExcludesVenuesOutsideRadius() {
    double lat = 41.8781;
    double lng = -87.6298;

    saveVenue("Inside",  lat, lng + 0.01);  // ~830 m
    saveVenue("Outside", lat, lng + 0.10);  // ~8.3 km

    Page<VenueDistanceProjection> page = venueRepository.findNearbyWithDistance(
        lat, lng, 2_000, PageRequest.of(0, 10));

    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent().get(0).getName()).isEqualTo("Inside");
  }

  @Test
  void findNearbyExcludesInactiveAndNullLocationVenues() {
    double lat = 41.8781;
    double lng = -87.6298;

    saveVenue("Active",   lat, lng + 0.01);
    Venue inactive = saveVenue("Inactive", lat, lng + 0.01);
    inactive.setActive(false);
    venueRepository.save(inactive);

    Venue nullLoc = saveVenue("NullLoc", lat, lng + 0.01);
    nullLoc.setLocation(null);
    venueRepository.save(nullLoc);

    em.flush();
    em.clear();

    Page<VenueDistanceProjection> page = venueRepository.findNearbyWithDistance(
        lat, lng, 5_000, PageRequest.of(0, 10));

    assertThat(page.getContent())
        .extracting(VenueDistanceProjection::getName)
        .containsExactly("Active");
  }

  // --- helpers -------------------------------------------------------------

  private Venue saveVenue(String name, double lat, double lng) {
    Venue v = new Venue();
    v.setName(name);
    v.setAddress(name + " St");
    v.setOwner(owner);
    v.setOpeningHours(Map.of());
    v.setAmenities(java.util.List.of());
    Point p = GF.createPoint(new Coordinate(lng, lat));
    p.setSRID(4326);
    v.setLocation(p);
    v.setActive(true);
    return venueRepository.save(v);
  }
}
