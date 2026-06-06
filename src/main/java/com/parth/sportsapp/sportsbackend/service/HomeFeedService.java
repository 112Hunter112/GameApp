package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.HomeFeedResponse;
import com.parth.sportsapp.sportsbackend.dto.SportSummary;
import com.parth.sportsapp.sportsbackend.dto.UserPreferenceResponse;
import com.parth.sportsapp.sportsbackend.dto.VenueNearbyResponse;
import com.parth.sportsapp.sportsbackend.mapper.VenueMapper;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.model.UserPreference;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import com.parth.sportsapp.sportsbackend.repository.UserPreferenceRepository;
import com.parth.sportsapp.sportsbackend.repository.VenueDistanceProjection;
import com.parth.sportsapp.sportsbackend.repository.VenueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds the home-screen payload: the user's preferred sports, the venues
 * near them that host those sports, and a discovery row of sports they don't
 * yet play but which have nearby courts.
 */
@Service
public class HomeFeedService {

  private static final double DEFAULT_RADIUS_METERS = 10_000d;
  private static final double MAX_RADIUS_METERS     = 50_000d;
  private static final int DEFAULT_VENUE_LIMIT      = 10;
  private static final int MAX_VENUE_LIMIT          = 50;

  private final UserPreferenceRepository preferenceRepository;
  private final VenueRepository venueRepository;
  private final SportsRepository sportsRepository;
  private final VenueMapper venueMapper;

  public HomeFeedService(UserPreferenceRepository preferenceRepository,
                         VenueRepository venueRepository,
                         SportsRepository sportsRepository,
                         VenueMapper venueMapper) {
    this.preferenceRepository = preferenceRepository;
    this.venueRepository = venueRepository;
    this.sportsRepository = sportsRepository;
    this.venueMapper = venueMapper;
  }

  @Transactional(readOnly = true)
  public HomeFeedResponse build(UUID userId,
                                double lat, double lng,
                                Double radiusMeters,
                                UUID sportIdFilter,
                                Integer limit) {

    // 1. Load the user's preferences.
    List<UserPreference> prefs = preferenceRepository.findByUserId(userId);

    // Sort: primary sport first, then by Elo desc, then by sport name.
    Comparator<UserPreference> primaryFirst = Comparator
        .comparing((UserPreference p) -> Boolean.TRUE.equals(p.getIsPrimarySport())).reversed()
        .thenComparing(p -> p.getEloRating() == null ? Integer.MIN_VALUE : -p.getEloRating())
        .thenComparing(p -> p.getSports() == null ? "" : p.getSports().getSportName());
    List<UserPreference> sortedPrefs = prefs.stream().sorted(primaryFirst).toList();

    List<UserPreferenceResponse> preferenceDtos = sortedPrefs.stream()
        .map(UserPreferenceService::toResponse)
        .toList();

    List<SportSummary> preferredSports = sortedPrefs.stream()
        .filter(p -> p.getSports() != null)
        .map(p -> new SportSummary(
            p.getSports().getId(),
            p.getSports().getSportName(),
            p.getSports().getIconURL(),
            p.getEloRating(),
            Boolean.TRUE.equals(p.getIsPrimarySport())))
        .toList();

    // 2. Decide which sports we filter venues by.
    List<UUID> filterSportIds;
    if (sportIdFilter != null) {
      filterSportIds = List.of(sportIdFilter);
    } else {
      filterSportIds = sortedPrefs.stream()
          .filter(p -> p.getSports() != null)
          .map(p -> p.getSports().getId())
          .distinct()
          .toList();
    }

    // 3. Effective radius: explicit > primary sport's saved max > default. Capped at 50km.
    double effectiveRadius = pickRadius(radiusMeters, sortedPrefs);
    int effectiveLimit = clamp(limit == null ? DEFAULT_VENUE_LIMIT : limit, 1, MAX_VENUE_LIMIT);

    // 4. Nearby venues hosting one of those sports.
    List<VenueNearbyResponse> venues;
    if (filterSportIds.isEmpty()) {
      venues = List.of();
    } else {
      Page<VenueDistanceProjection> page = venueRepository.findNearbyForSports(
          lat, lng, effectiveRadius, filterSportIds, PageRequest.of(0, effectiveLimit));
      venues = page.getContent().stream()
          .map(venueMapper::toNearbyResponse)
          .toList();
    }

    // 5. Discovery: sports with courts in range that the user doesn't already play.
    Set<UUID> preferredSportIds = preferredSports.stream()
        .map(SportSummary::getSportId)
        .collect(Collectors.toSet());

    List<SportSummary> availableNearby = sportsRepository.findActiveSportsNearby(lat, lng, effectiveRadius)
        .stream()
        .map(row -> new SportSummary(
            (UUID) row[0],
            (String) row[1],
            (String) row[2],
            null,                // discovery — no Elo yet
            null))               // not primary
        .filter(s -> !preferredSportIds.contains(s.getSportId()))
        .toList();

    return new HomeFeedResponse(preferredSports, preferenceDtos, venues, availableNearby);
  }

  // --- helpers --------------------------------------------------------------

  private double pickRadius(Double explicit, List<UserPreference> sortedPrefs) {
    if (explicit != null) {
      return clamp(explicit, 1d, MAX_RADIUS_METERS);
    }
    return sortedPrefs.stream()
        .filter(p -> Boolean.TRUE.equals(p.getIsPrimarySport()))
        .findFirst()
        .map(UserPreference::getMaxTravelDistanceMeters)
        .filter(v -> v != null && v > 0)
        .map(Integer::doubleValue)
        .map(v -> Math.min(v, MAX_RADIUS_METERS))
        .orElse(DEFAULT_RADIUS_METERS);
  }

  private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
  private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
