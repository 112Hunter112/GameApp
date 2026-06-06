package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.UserPreferenceRequest;
import com.parth.sportsapp.sportsbackend.dto.UserPreferenceResponse;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserPreference;
import com.parth.sportsapp.sportsbackend.model.UserPreferenceId;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import com.parth.sportsapp.sportsbackend.repository.UserPreferenceRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Manages a user's per-sport preferences.
 *
 * <p>All write methods are scoped to a {@code userId} pulled from the JWT in
 * the controller — there is no API path that lets one user read or mutate
 * another user's preferences.</p>
 */
@Service
public class UserPreferenceService {

  private static final int DEFAULT_ELO = 1200;

  private final UserPreferenceRepository preferenceRepository;
  private final SportsRepository sportsRepository;
  private final UserRepository userRepository;

  public UserPreferenceService(UserPreferenceRepository preferenceRepository,
                               SportsRepository sportsRepository,
                               UserRepository userRepository) {
    this.preferenceRepository = preferenceRepository;
    this.sportsRepository = sportsRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<UserPreferenceResponse> listMine(UUID userId) {
    return preferenceRepository.findByUserId(userId).stream()
        .map(UserPreferenceService::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public UserPreferenceResponse get(UUID userId, UUID sportId) {
    UserPreference p = preferenceRepository.findByUser_IdAndSports_Id(userId, sportId)
        .orElseThrow(() -> new NotFoundException("Preference not found"));
    return toResponse(p);
  }

  /**
   * Create-or-update. Fields that are null in the request leave the entity
   * unchanged on update (partial update semantics); on create, nulls become
   * sensible defaults where the column is non-nullable.
   */
  @Transactional
  public UserPreferenceResponse upsert(UUID userId, UserPreferenceRequest req) {
    Sports sport = sportsRepository.findById(req.getSportId())
        .orElseThrow(() -> new NotFoundException("Sport not found"));

    UserPreferenceId pk = new UserPreferenceId();
    pk.setUserId(userId);
    pk.setSportId(sport.getId());

    UserPreference pref = preferenceRepository.findById(pk).orElseGet(() -> {
      // Verify user exists once on creation.
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new NotFoundException("User not found"));
      UserPreference p = new UserPreference();
      p.setId(pk);
      p.setUser(user);
      p.setSports(sport);
      // Sensible defaults for non-null columns on first insert.
      p.setEloRating(DEFAULT_ELO);
      p.setMatchesPlayed(0);
      p.setMatchesWon(0);
      p.setOpenToMatchmaking(true);
      p.setAvailableWeekdays(true);
      p.setAvailableWeekends(true);
      p.setIsPrimarySport(false);
      return p;
    });

    applyRequest(pref, req);
    pref.setLastActiveAt(LocalDateTime.now());

    return toResponse(preferenceRepository.save(pref));
  }

  /**
   * Atomically demote the current primary (if any) and promote {@code sportId}.
   * Wrapped in a single transaction so a crash mid-way cannot leave the user
   * with zero or two primary sports.
   */
  @Transactional
  public void setPrimary(UUID userId, UUID sportId) {
    UserPreference target = preferenceRepository.findByUser_IdAndSports_Id(userId, sportId)
        .orElseThrow(() -> new NotFoundException("Preference not found"));

    preferenceRepository.clearPrimaryForUser(userId);
    target.setIsPrimarySport(true);
    preferenceRepository.save(target);
  }

  @Transactional
  public void delete(UUID userId, UUID sportId) {
    UserPreference p = preferenceRepository.findByUser_IdAndSports_Id(userId, sportId)
        .orElseThrow(() -> new NotFoundException("Preference not found"));
    preferenceRepository.delete(p);
  }

  // --- helpers --------------------------------------------------------------

  private static void applyRequest(UserPreference p, UserPreferenceRequest r) {
    if (r.getProficiencyLevel()         != null) p.setProficiencyLevel(r.getProficiencyLevel());
    if (r.getYearsPlaying()             != null) p.setYearsPlaying(r.getYearsPlaying());
    if (r.getPreferredFormat()          != null) p.setPreferredFormat(r.getPreferredFormat());
    if (r.getPreferredOpponentGender()  != null) p.setPreferredOpponentGender(r.getPreferredOpponentGender());
    if (r.getMinOpponentAge()           != null) p.setMinOpponentAge(r.getMinOpponentAge());
    if (r.getMaxOpponentAge()           != null) p.setMaxOpponentAge(r.getMaxOpponentAge());
    if (r.getMinOpponentElo()           != null) p.setMinOpponentElo(r.getMinOpponentElo());
    if (r.getMaxOpponentElo()           != null) p.setMaxOpponentElo(r.getMaxOpponentElo());
    if (r.getMaxTravelDistanceMeters()  != null) p.setMaxTravelDistanceMeters(r.getMaxTravelDistanceMeters());
    if (r.getOpenToMatchmaking()        != null) p.setOpenToMatchmaking(r.getOpenToMatchmaking());
    if (r.getAvailableWeekdays()        != null) p.setAvailableWeekdays(r.getAvailableWeekdays());
    if (r.getAvailableWeekends()        != null) p.setAvailableWeekends(r.getAvailableWeekends());
    if (r.getNotificationRadiusMeters() != null) p.setNotificationRadiusMeters(r.getNotificationRadiusMeters());
  }

  static UserPreferenceResponse toResponse(UserPreference p) {
    UserPreferenceResponse r = new UserPreferenceResponse();
    r.setSportId(p.getSports() == null ? null : p.getSports().getId());
    r.setSportName(p.getSports() == null ? null : p.getSports().getSportName());
    r.setProficiencyLevel(p.getProficiencyLevel());
    r.setEloRating(p.getEloRating());
    r.setMatchesPlayed(p.getMatchesPlayed());
    r.setMatchesWon(p.getMatchesWon());
    r.setYearsPlaying(p.getYearsPlaying());
    r.setPreferredFormat(p.getPreferredFormat());
    r.setPreferredOpponentGender(p.getPreferredOpponentGender());
    r.setMinOpponentAge(p.getMinOpponentAge());
    r.setMaxOpponentAge(p.getMaxOpponentAge());
    r.setMinOpponentElo(p.getMinOpponentElo());
    r.setMaxOpponentElo(p.getMaxOpponentElo());
    r.setMaxTravelDistanceMeters(p.getMaxTravelDistanceMeters());
    r.setOpenToMatchmaking(p.getOpenToMatchmaking());
    r.setAvailableWeekdays(p.getAvailableWeekdays());
    r.setAvailableWeekends(p.getAvailableWeekends());
    r.setNotificationRadiusMeters(p.getNotificationRadiusMeters());
    r.setIsPrimarySport(p.getIsPrimarySport());
    r.setLastActiveAt(p.getLastActiveAt());
    r.setCreatedAt(p.getCreatedAt());
    r.setUpdatedAt(p.getUpdatedAt());
    return r;
  }
}
