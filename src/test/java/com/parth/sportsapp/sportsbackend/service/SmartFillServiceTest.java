package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.SmartFillRequest;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.model.Courts;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.repository.CourtRepository;
import com.parth.sportsapp.sportsbackend.repository.SmartFillCandidateProjection;
import com.parth.sportsapp.sportsbackend.repository.UserPreferenceRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartFillServiceTest {

  private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

  @Mock CourtRepository courtRepository;
  @Mock UserPreferenceRepository userPreferenceRepository;
  @Mock UserRepository userRepository;
  @Mock NotificationService notificationService;
  @Mock com.parth.sportsapp.sportsbackend.repository.SmartFillOfferRepository offerRepository;

  SmartFillService service;

  UUID vendorId;
  UUID courtId;

  @BeforeEach
  void setUp() {
    service = new SmartFillService(
        courtRepository, userPreferenceRepository, userRepository,
        notificationService, offerRepository);
    vendorId = UUID.randomUUID();
    courtId = UUID.randomUUID();
  }

  @Test
  void notifiesMatchingCandidates() {
    Courts court = court(vendorId, /*withLocation*/ true);
    when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));

    UUID playerId = UUID.randomUUID();
    when(userPreferenceRepository.findSmartFillCandidates(
        any(), anyDouble(), anyDouble(), anyBoolean(), anyInt()))
        .thenReturn(List.of(candidate(playerId)));
    User player = new User();
    player.setId(playerId);
    when(userRepository.findById(playerId)).thenReturn(Optional.of(player));

    int notified = service.offerSlot(vendorId, courtId, slot(2, 3, 30));

    assertThat(notified).isEqualTo(1);
    verify(notificationService).sendSmartFillOffer(eq(player), eq(courtId), contains("30% off"));
  }

  @Test
  void rejectsCourtOwnedByAnotherVendor() {
    Courts court = court(UUID.randomUUID(), true); // different owner
    when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));

    assertThatThrownBy(() -> service.offerSlot(vendorId, courtId, slot(2, 3, null)))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(notificationService);
  }

  @Test
  void rejectsPastSlot() {
    SmartFillRequest req = new SmartFillRequest();
    req.setSlotStart(LocalDateTime.now().minusHours(1));
    req.setSlotEnd(LocalDateTime.now().plusHours(1));

    assertThatThrownBy(() -> service.offerSlot(vendorId, courtId, req))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("future");
  }

  @Test
  void rejectsInvertedSlot() {
    SmartFillRequest req = new SmartFillRequest();
    req.setSlotStart(LocalDateTime.now().plusHours(3));
    req.setSlotEnd(LocalDateTime.now().plusHours(2));

    assertThatThrownBy(() -> service.offerSlot(vendorId, courtId, req))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void rejectsVenueWithoutLocation() {
    Courts court = court(vendorId, /*withLocation*/ false);
    when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));

    assertThatThrownBy(() -> service.offerSlot(vendorId, courtId, slot(2, 3, null)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("location");
  }

  @Test
  void dedupSkipsPlayersAlreadyNotifiedForThisSlot() {
    Courts court = court(vendorId, true);
    when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));

    UUID oldPlayer = UUID.randomUUID();
    UUID newPlayer = UUID.randomUUID();
    SmartFillRequest req = slot(2, 3, null);

    // oldPlayer was already notified for this exact court + slot.
    when(offerRepository.findNotifiedUserIds(eq(courtId), eq(req.getSlotStart())))
        .thenReturn(List.of(oldPlayer));
    when(userPreferenceRepository.findSmartFillCandidates(
        any(), anyDouble(), anyDouble(), anyBoolean(), anyInt()))
        .thenReturn(List.of(candidate(oldPlayer), candidate(newPlayer)));
    User u = new User();
    u.setId(newPlayer);
    when(userRepository.findById(newPlayer)).thenReturn(Optional.of(u));

    int notified = service.offerSlot(vendorId, courtId, req);

    // Only the NEW player gets the offer; the old one is never re-notified.
    assertThat(notified).isEqualTo(1);
    verify(userRepository, never()).findById(oldPlayer);
    verify(notificationService, times(1)).sendSmartFillOffer(any(), eq(courtId), any());
  }

  @Test
  void cooldownRejectsFourthDistinctSlotInOneDay() {
    Courts court = court(vendorId, true);
    when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));

    SmartFillRequest req = slot(2, 3, null);
    // A brand-new slot (nobody notified yet) while 3 distinct slots were
    // already offered today -> over the daily cap.
    when(offerRepository.findNotifiedUserIds(eq(courtId), eq(req.getSlotStart())))
        .thenReturn(List.of());
    when(offerRepository.countDistinctSlotsSince(eq(courtId), any()))
        .thenReturn((long) SmartFillService.MAX_SLOTS_PER_COURT_PER_DAY);

    assertThatThrownBy(() -> service.offerSlot(vendorId, courtId, req))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("limit");
    verifyNoInteractions(notificationService);
  }

  @Test
  void zeroCandidatesIsNotAnError() {
    Courts court = court(vendorId, true);
    when(courtRepository.findById(courtId)).thenReturn(Optional.of(court));
    when(userPreferenceRepository.findSmartFillCandidates(
        any(), anyDouble(), anyDouble(), anyBoolean(), anyInt()))
        .thenReturn(List.of());

    assertThat(service.offerSlot(vendorId, courtId, slot(2, 3, null))).isZero();
    verifyNoInteractions(notificationService);
  }

  // --- helpers --------------------------------------------------------------

  private SmartFillRequest slot(int startHoursFromNow, int endHoursFromNow, Integer discount) {
    SmartFillRequest r = new SmartFillRequest();
    r.setSlotStart(LocalDateTime.now().plusHours(startHoursFromNow));
    r.setSlotEnd(LocalDateTime.now().plusHours(endHoursFromNow));
    r.setDiscountPercent(discount);
    return r;
  }

  private Courts court(UUID ownerId, boolean withLocation) {
    User owner = new User();
    owner.setId(ownerId);

    Venue venue = new Venue();
    venue.setId(UUID.randomUUID());
    venue.setName("Seed Venue");
    venue.setOwner(owner);
    if (withLocation) {
      Point p = GF.createPoint(new Coordinate(-87.65, 41.85));
      p.setSRID(4326);
      venue.setLocation(p);
    }

    Sports sport = new Sports();
    sport.setId(UUID.randomUUID());
    sport.setSportName("Tennis");

    Courts c = new Courts();
    c.setVenue(venue);
    c.setSports(sport);
    c.setCourtNumber("Court 1");
    return c;
  }

  private SmartFillCandidateProjection candidate(UUID userId) {
    return new SmartFillCandidateProjection() {
      @Override public UUID getUserId() { return userId; }
      @Override public String getFirstName() { return "Player"; }
    };
  }
}
