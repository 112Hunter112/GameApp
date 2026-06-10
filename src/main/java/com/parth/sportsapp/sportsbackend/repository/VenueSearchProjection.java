package com.parth.sportsapp.sportsbackend.repository;

import java.util.UUID;

/** Row shape of VenueRepository#discoverVenues (native query projection). */
public interface VenueSearchProjection {

  UUID getId();
  String getName();
  String getAddress();
  Double getDistanceMeters();   // null when the caller sent no location
}
