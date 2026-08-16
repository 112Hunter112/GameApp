package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.BookingPolicyRequest;
import com.parth.sportsapp.sportsbackend.dto.BookingPolicyResponse;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.BookingPolicy;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.repository.BookingPolicyRepository;
import com.parth.sportsapp.sportsbackend.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPolicyServiceTest {

  @Mock private BookingPolicyRepository bookingPolicyRepository;
  @Mock private VenueRepository venueRepository;

  @InjectMocks private BookingPolicyService service;

  private User owner;
  private Venue venue;

  @BeforeEach
  void setUp() {
    owner = new User();
    owner.setId(UUID.randomUUID());

    venue = new Venue();
    venue.setId(UUID.randomUUID());
    venue.setName("Riverside Sports Hub");
    venue.setOwner(owner);
  }

  // --- getPolicy -------------------------------------------------------------

  @Test
  void getPolicyRejectsUnknownVenue() {
    UUID venueId = UUID.randomUUID();
    when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getPolicy(venueId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void getPolicyFallsBackToPlatformDefaults() {
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
    when(bookingPolicyRepository.findByVenue_Id(venue.getId())).thenReturn(Optional.empty());

    BookingPolicyResponse res = service.getPolicy(venue.getId());

    // Booking must work out of the box for venues that never configured rules.
    assertThat(res.isCustomized()).isFalse();
    assertThat(res.getSlotIncrementMinutes()).isEqualTo(BookingPolicy.DEFAULT_INCREMENT);
    assertThat(res.getMinBookingMinutes()).isEqualTo(BookingPolicy.DEFAULT_MIN_MINUTES);
    assertThat(res.getMaxBookingMinutes()).isEqualTo(BookingPolicy.DEFAULT_MAX_MINUTES);
    assertThat(res.getAdvanceBookingDays()).isEqualTo(BookingPolicy.DEFAULT_ADVANCE_DAYS);
    assertThat(res.isAutoConfirm()).isTrue();
    assertThat(res.getOpeningHours())
        .hasSize(7)
        .containsEntry("MONDAY", BookingPolicy.DEFAULT_DAY_HOURS)
        .containsEntry("SUNDAY", BookingPolicy.DEFAULT_DAY_HOURS);
  }

  @Test
  void getPolicyReturnsSavedPolicyAsCustomized() {
    BookingPolicy saved = BookingPolicy.defaults(venue);
    saved.setSlotIncrementMinutes(30);
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
    when(bookingPolicyRepository.findByVenue_Id(venue.getId())).thenReturn(Optional.of(saved));

    BookingPolicyResponse res = service.getPolicy(venue.getId());

    assertThat(res.isCustomized()).isTrue();
    assertThat(res.getSlotIncrementMinutes()).isEqualTo(30);
    assertThat(res.getVenueId()).isEqualTo(venue.getId());
  }

  @Test
  void getEffectivePolicyPrefersSavedOverDefaults() {
    BookingPolicy saved = BookingPolicy.defaults(venue);
    when(bookingPolicyRepository.findByVenue_Id(venue.getId())).thenReturn(Optional.of(saved));
    assertThat(service.getEffectivePolicy(venue)).isSameAs(saved);

    when(bookingPolicyRepository.findByVenue_Id(venue.getId())).thenReturn(Optional.empty());
    BookingPolicy fallback = service.getEffectivePolicy(venue);
    assertThat(fallback.getVenue()).isSameAs(venue);
    assertThat(fallback.getSlotIncrementMinutes()).isEqualTo(BookingPolicy.DEFAULT_INCREMENT);
  }

  // --- updatePolicy: access control -------------------------------------------

  @Test
  void updatePolicyRejectsUnknownVenue() {
    UUID venueId = UUID.randomUUID();
    when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updatePolicy(venueId, owner.getId(), validRequest()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void updatePolicyRejectsNonOwner() {
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));

    assertThatThrownBy(() -> service.updatePolicy(venue.getId(), UUID.randomUUID(), validRequest()))
        .isInstanceOf(ForbiddenException.class);

    verify(bookingPolicyRepository, never()).save(any());
  }

  // --- updatePolicy: upsert ----------------------------------------------------

  @Test
  void updatePolicyCreatesPolicyOnFirstSave() {
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
    when(bookingPolicyRepository.findByVenue_Id(venue.getId())).thenReturn(Optional.empty());
    when(bookingPolicyRepository.save(any(BookingPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

    BookingPolicyRequest request = validRequest();
    request.setAutoConfirm(false); // request-to-book venue

    BookingPolicyResponse res = service.updatePolicy(venue.getId(), owner.getId(), request);

    assertThat(res.isCustomized()).isTrue();
    assertThat(res.getVenueId()).isEqualTo(venue.getId());
    assertThat(res.isAutoConfirm()).isFalse();
    assertThat(res.getSlotIncrementMinutes()).isEqualTo(30);
    assertThat(res.getMinBookingMinutes()).isEqualTo(60);
    assertThat(res.getMaxBookingMinutes()).isEqualTo(120);
  }

  @Test
  void updatePolicyOverwritesExistingPolicy() {
    BookingPolicy existing = BookingPolicy.defaults(venue);
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
    when(bookingPolicyRepository.findByVenue_Id(venue.getId())).thenReturn(Optional.of(existing));
    when(bookingPolicyRepository.save(any(BookingPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

    service.updatePolicy(venue.getId(), owner.getId(), validRequest());

    // The same row is updated in place — no second policy for the venue.
    verify(bookingPolicyRepository).save(existing);
    assertThat(existing.getSlotIncrementMinutes()).isEqualTo(30);
    assertThat(existing.getMaxBookingMinutes()).isEqualTo(120);
  }

  @Test
  void updatePolicyAllowsHalfHourSessionsOnThirtyMinuteGrid() {
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
    when(bookingPolicyRepository.findByVenue_Id(venue.getId())).thenReturn(Optional.empty());
    when(bookingPolicyRepository.save(any(BookingPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

    // 90-minute sessions are exactly the case a 30-minute grid exists for.
    BookingPolicyRequest request = validRequest();
    request.setSlotIncrementMinutes(30);
    request.setMinBookingMinutes(90);
    request.setMaxBookingMinutes(150);

    BookingPolicyResponse res = service.updatePolicy(venue.getId(), owner.getId(), request);

    assertThat(res.getMinBookingMinutes()).isEqualTo(90);
    assertThat(res.getMaxBookingMinutes()).isEqualTo(150);
  }

  // --- updatePolicy: validation -------------------------------------------------

  @Test
  void updatePolicyRejectsUnsupportedIncrement() {
    stubOwnedVenue();
    BookingPolicyRequest request = validRequest();
    request.setSlotIncrementMinutes(45);

    assertThatThrownBy(() -> service.updatePolicy(venue.getId(), owner.getId(), request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("30 or 60");
  }

  @Test
  void updatePolicyRejectsLengthsOffTheGrid() {
    stubOwnedVenue();
    BookingPolicyRequest request = validRequest(); // 60-minute grid
    request.setMinBookingMinutes(90);              // not a multiple of 60
    request.setSlotIncrementMinutes(60);

    assertThatThrownBy(() -> service.updatePolicy(venue.getId(), owner.getId(), request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("multiple of the slot increment");
  }

  @Test
  void updatePolicyRejectsMinAboveMax() {
    stubOwnedVenue();
    BookingPolicyRequest request = validRequest();
    request.setMinBookingMinutes(180);
    request.setMaxBookingMinutes(60);

    assertThatThrownBy(() -> service.updatePolicy(venue.getId(), owner.getId(), request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("cannot exceed the maximum");
  }

  @Test
  void updatePolicyRejectsAllDaysClosed() {
    stubOwnedVenue();
    BookingPolicyRequest request = validRequest();
    request.setOpeningHours(new HashMap<>());

    assertThatThrownBy(() -> service.updatePolicy(venue.getId(), owner.getId(), request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("At least one day");
  }

  @Test
  void updatePolicyRejectsUnknownDayName() {
    stubOwnedVenue();
    BookingPolicyRequest request = validRequest();
    request.setOpeningHours(Map.of("FUNDAY", "08:00-22:00"));

    assertThatThrownBy(() -> service.updatePolicy(venue.getId(), owner.getId(), request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unknown day");
  }

  @Test
  void updatePolicyRejectsMalformedHours() {
    stubOwnedVenue();

    // Single-digit hour misses the HH:mm-HH:mm shape.
    BookingPolicyRequest request = validRequest();
    request.setOpeningHours(Map.of("MONDAY", "8:00-22:00"));
    assertThatThrownBy(() -> service.updatePolicy(venue.getId(), owner.getId(), request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("must look like");

    BookingPolicyRequest garbage = validRequest();
    garbage.setOpeningHours(Map.of("MONDAY", "whenever"));
    assertThatThrownBy(() -> service.updatePolicy(venue.getId(), owner.getId(), garbage))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("must look like");
  }

  @Test
  void updatePolicyRejectsClosingBeforeOpening() {
    stubOwnedVenue();

    BookingPolicyRequest inverted = validRequest();
    inverted.setOpeningHours(Map.of("MONDAY", "22:00-08:00"));
    assertThatThrownBy(() -> service.updatePolicy(venue.getId(), owner.getId(), inverted))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Closing time must be after");

    BookingPolicyRequest zeroLength = validRequest();
    zeroLength.setOpeningHours(Map.of("MONDAY", "08:00-08:00"));
    assertThatThrownBy(() -> service.updatePolicy(venue.getId(), owner.getId(), zeroLength))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Closing time must be after");
  }

  // --- helpers ---------------------------------------------------------------

  private void stubOwnedVenue() {
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
  }

  /** A request that passes every validation rule (30-min grid, 1–2h sessions). */
  private BookingPolicyRequest validRequest() {
    BookingPolicyRequest r = new BookingPolicyRequest();
    r.setSlotIncrementMinutes(30);
    r.setMinBookingMinutes(60);
    r.setMaxBookingMinutes(120);
    r.setAdvanceBookingDays(14);
    r.setMinNoticeMinutes(60);
    r.setCancellationCutoffMinutes(120);
    r.setAutoConfirm(true);
    r.setOpeningHours(Map.of(
        "MONDAY", "08:00-22:00",
        "SATURDAY", "09:00-18:00"));
    return r;
  }
}
