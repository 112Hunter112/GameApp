package com.parth.sportsapp.sportsbackend.dto;

import java.util.List;

/**
 * The single payload the home screen renders.
 *
 * <ul>
 *   <li>{@code preferredSports} — lightweight chip list of the user's sports
 *       (primary first), for the "Your sports" row at the top.</li>
 *   <li>{@code preferences} — full detail per sport (Elo, matches, filters)
 *       for the settings panel / details view.</li>
 *   <li>{@code nearbyVenues} — venues near the user that host courts for one
 *       of their preferred sports.</li>
 *   <li>{@code availableSportsNearby} — sports that have at least one court
 *       within the user's radius but which the user has NOT yet added to
 *       their preferences. Powers a "Discover new sports near you" strip.</li>
 * </ul>
 */
public class HomeFeedResponse {

  private List<SportSummary> preferredSports;
  private List<UserPreferenceResponse> preferences;
  private List<VenueNearbyResponse> nearbyVenues;
  private List<SportSummary> availableSportsNearby;

  public HomeFeedResponse() {}

  public HomeFeedResponse(List<SportSummary> preferredSports,
                          List<UserPreferenceResponse> preferences,
                          List<VenueNearbyResponse> nearbyVenues,
                          List<SportSummary> availableSportsNearby) {
    this.preferredSports = preferredSports;
    this.preferences = preferences;
    this.nearbyVenues = nearbyVenues;
    this.availableSportsNearby = availableSportsNearby;
  }

  public List<SportSummary> getPreferredSports() { return preferredSports; }
  public void setPreferredSports(List<SportSummary> v) { this.preferredSports = v; }

  public List<UserPreferenceResponse> getPreferences() { return preferences; }
  public void setPreferences(List<UserPreferenceResponse> v) { this.preferences = v; }

  public List<VenueNearbyResponse> getNearbyVenues() { return nearbyVenues; }
  public void setNearbyVenues(List<VenueNearbyResponse> v) { this.nearbyVenues = v; }

  public List<SportSummary> getAvailableSportsNearby() { return availableSportsNearby; }
  public void setAvailableSportsNearby(List<SportSummary> v) { this.availableSportsNearby = v; }
}
