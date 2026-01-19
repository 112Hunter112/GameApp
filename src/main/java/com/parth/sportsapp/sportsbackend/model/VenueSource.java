package com.parth.sportsapp.sportsbackend.model;

public enum VenueSource {
  REGISTERED,      // Manually added by a Vendor
  AUTO_CREATED,    // Pin drops (Unknown source)
  GOOGLE_PLACES,   // Imported via Place ID
  OSM              // Imported via OpenStreetMap
}
