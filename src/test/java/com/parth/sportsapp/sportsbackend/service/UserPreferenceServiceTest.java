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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

  @Mock UserPreferenceRepository preferenceRepository;
  @Mock SportsRepository sportsRepository;
  @Mock UserRepository userRepository;

  UserPreferenceService service;

  @BeforeEach
  void setUp() {
    service = new UserPreferenceService(preferenceRepository, sportsRepository, userRepository);
  }

  // --- upsert --------------------------------------------------------------

  @Test
  void upsertCreatesPreferenceWithDefaultEloOnFirstInsert() {
    UUID userId = UUID.randomUUID();
    Sports tennis = sport("Tennis");
    when(sportsRepository.findById(tennis.getId())).thenReturn(Optional.of(tennis));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId)));
    when(preferenceRepository.findById(any(UserPreferenceId.class))).thenReturn(Optional.empty());
    when(preferenceRepository.save(any(UserPreference.class))).thenAnswer(inv -> inv.getArgument(0));

    UserPreferenceRequest req = new UserPreferenceRequest();
    req.setSportId(tennis.getId());
    req.setProficiencyLevel("INTERMEDIATE");

    UserPreferenceResponse res = service.upsert(userId, req);

    assertThat(res.getEloRating()).isEqualTo(1200);          // default applied
    assertThat(res.getMatchesPlayed()).isEqualTo(0);
    assertThat(res.getProficiencyLevel()).isEqualTo("INTERMEDIATE");
    assertThat(res.getSportName()).isEqualTo("Tennis");
    assertThat(res.getIsPrimarySport()).isFalse();
    assertThat(res.getOpenToMatchmaking()).isTrue();
  }

  @Test
  void upsertPreservesEloOnUpdate() {
    UUID userId = UUID.randomUUID();
    Sports tennis = sport("Tennis");
    UserPreference existing = pref(userId, tennis, /* elo */ 1750, /* primary */ true);
    when(sportsRepository.findById(tennis.getId())).thenReturn(Optional.of(tennis));
    when(preferenceRepository.findById(any(UserPreferenceId.class))).thenReturn(Optional.of(existing));
    when(preferenceRepository.save(any(UserPreference.class))).thenAnswer(inv -> inv.getArgument(0));

    UserPreferenceRequest req = new UserPreferenceRequest();
    req.setSportId(tennis.getId());
    req.setYearsPlaying(7);

    UserPreferenceResponse res = service.upsert(userId, req);

    // Elo and isPrimarySport are NOT in the request and must NOT be reset.
    assertThat(res.getEloRating()).isEqualTo(1750);
    assertThat(res.getIsPrimarySport()).isTrue();
    assertThat(res.getYearsPlaying()).isEqualTo(7);
  }

  @Test
  void upsertRejectsUnknownSport() {
    UUID userId = UUID.randomUUID();
    UUID badSport = UUID.randomUUID();
    when(sportsRepository.findById(badSport)).thenReturn(Optional.empty());

    UserPreferenceRequest req = new UserPreferenceRequest();
    req.setSportId(badSport);

    assertThatThrownBy(() -> service.upsert(userId, req))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Sport");
  }

  // --- setPrimary ----------------------------------------------------------

  @Test
  void setPrimaryDemotesPreviousAndPromotesTarget() {
    UUID userId = UUID.randomUUID();
    Sports badminton = sport("Badminton");
    UserPreference target = pref(userId, badminton, 1300, /* primary */ false);
    when(preferenceRepository.findByUser_IdAndSports_Id(userId, badminton.getId()))
        .thenReturn(Optional.of(target));

    service.setPrimary(userId, badminton.getId());

    // 1. Clear primary for all of this user's preferences
    verify(preferenceRepository).clearPrimaryForUser(userId);
    // 2. Save the target with isPrimary=true
    ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
    verify(preferenceRepository).save(captor.capture());
    assertThat(captor.getValue().getIsPrimarySport()).isTrue();
  }

  @Test
  void setPrimaryRejectsWhenPreferenceMissing() {
    UUID userId = UUID.randomUUID();
    UUID sportId = UUID.randomUUID();
    when(preferenceRepository.findByUser_IdAndSports_Id(userId, sportId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.setPrimary(userId, sportId))
        .isInstanceOf(NotFoundException.class);

    verify(preferenceRepository, never()).clearPrimaryForUser(any());
    verify(preferenceRepository, never()).save(any());
  }

  // --- delete --------------------------------------------------------------

  @Test
  void deleteRemovesOwnedPreference() {
    UUID userId = UUID.randomUUID();
    Sports tennis = sport("Tennis");
    UserPreference existing = pref(userId, tennis, 1500, false);
    when(preferenceRepository.findByUser_IdAndSports_Id(userId, tennis.getId()))
        .thenReturn(Optional.of(existing));

    service.delete(userId, tennis.getId());

    verify(preferenceRepository).delete(existing);
  }

  @Test
  void deleteRejectsWhenPreferenceMissing() {
    UUID userId = UUID.randomUUID();
    UUID sportId = UUID.randomUUID();
    when(preferenceRepository.findByUser_IdAndSports_Id(userId, sportId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(userId, sportId))
        .isInstanceOf(NotFoundException.class);
    verify(preferenceRepository, never()).delete((UserPreference) any());
  }

  // --- helpers -------------------------------------------------------------

  private Sports sport(String name) {
    Sports s = new Sports();
    s.setId(UUID.randomUUID());
    s.setSportName(name);
    return s;
  }

  private User user(UUID id) {
    User u = new User();
    u.setId(id);
    return u;
  }

  private UserPreference pref(UUID userId, Sports sport, int elo, boolean primary) {
    UserPreferenceId pk = new UserPreferenceId();
    pk.setUserId(userId);
    pk.setSportId(sport.getId());
    UserPreference p = new UserPreference();
    p.setId(pk);
    p.setUser(user(userId));
    p.setSports(sport);
    p.setEloRating(elo);
    p.setMatchesPlayed(0);
    p.setMatchesWon(0);
    p.setOpenToMatchmaking(true);
    p.setAvailableWeekdays(true);
    p.setAvailableWeekends(true);
    p.setIsPrimarySport(primary);
    return p;
  }
}
