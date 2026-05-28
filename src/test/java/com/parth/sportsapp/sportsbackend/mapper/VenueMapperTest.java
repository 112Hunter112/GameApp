package com.parth.sportsapp.sportsbackend.mapper;

import com.parth.sportsapp.sportsbackend.dto.VenueRequest;
import com.parth.sportsapp.sportsbackend.dto.VenueResponse;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link VenueMapper}. The hard-won invariant this protects:
 * JTS uses (x, y) = (longitude, latitude) — getting the order wrong puts
 * every venue in the Atlantic Ocean with no compile-time warning.
 */
class VenueMapperTest {

  private final VenueMapper mapper = new VenueMapper();

  @Test
  void toEntityStoresLngAsXAndLatAsY() {
    VenueRequest req = newRequest();
    req.setLatitude(41.85);    // Chicago
    req.setLongitude(-87.65);

    Venue venue = mapper.toEntity(req, owner());

    Point loc = venue.getLocation();
    assertThat(loc).isNotNull();
    assertThat(loc.getX()).isEqualTo(-87.65); // longitude -> x
    assertThat(loc.getY()).isEqualTo(41.85);  // latitude  -> y
    assertThat(loc.getSRID()).isEqualTo(4326);
  }

  @Test
  void toEntityWithNullCoordsLeavesLocationNull() {
    VenueRequest req = newRequest();
    req.setLatitude(null);
    req.setLongitude(null);

    Venue venue = mapper.toEntity(req, owner());

    assertThat(venue.getLocation()).isNull();
  }

  @Test
  void toResponseUnpacksPointBackToLatAndLng() {
    Venue venue = new Venue();
    venue.setId(UUID.randomUUID());
    venue.setName("Test Court");
    venue.setAddress("123 Main");
    venue.setOpeningHours(Map.of());
    venue.setAmenities(java.util.List.of());
    venue.setOwner(owner());

    // Round-trip through the mapper from a fresh request, then read it back.
    VenueRequest req = newRequest();
    req.setLatitude(41.85);
    req.setLongitude(-87.65);
    venue.setLocation(mapper.toEntity(req, owner()).getLocation());

    VenueResponse response = mapper.toResponse(venue);

    assertThat(response.getLatitude()).isEqualTo(41.85);
    assertThat(response.getLongitude()).isEqualTo(-87.65);
  }

  @Test
  void toResponseTolerantOfNullLocation() {
    Venue venue = new Venue();
    venue.setId(UUID.randomUUID());
    venue.setName("Geocoded later");
    venue.setOwner(owner());
    venue.setOpeningHours(Map.of());
    venue.setAmenities(java.util.List.of());
    venue.setLocation(null);

    VenueResponse response = mapper.toResponse(venue);

    assertThat(response.getLatitude()).isNull();
    assertThat(response.getLongitude()).isNull();
  }

  // --- helpers --------------------------------------------------------------

  private VenueRequest newRequest() {
    VenueRequest r = new VenueRequest();
    r.setName("Test Court");
    r.setAddress("123 Main St");
    r.setPhoneNumber("5551234567");
    r.setDescription("desc");
    r.setOpeningHours(Map.of("monday", "08:00-22:00"));
    return r;
  }

  private User owner() {
    User u = new User();
    u.setId(UUID.randomUUID());
    u.setFirstName("Test");
    u.setLastName("Owner");
    u.setEmail("owner@example.com");
    return u;
  }
}
