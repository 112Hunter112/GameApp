package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.*;
import com.parth.sportsapp.sportsbackend.repository.BookingRepository;
import com.parth.sportsapp.sportsbackend.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorDashboardServiceTest {

  @Mock BookingRepository bookingRepository;
  @Mock VenueRepository venueRepository;
  @Mock PlayerReliabilityService reliabilityService;
  @Mock NotificationService notificationService;

  VendorDashboardService service;

  UUID vendorId;
  UUID venueId;

  @BeforeEach
  void setUp() {
    service = new VendorDashboardService(
        bookingRepository, venueRepository, reliabilityService, notificationService);
    vendorId = UUID.randomUUID();
    venueId = UUID.randomUUID();
  }

  // --- ownership (the IDOR guard) -------------------------------------------

  @Test
  void dashboardRejectsVenueOwnedBySomeoneElse() {
    Venue venue = venue(venueId, UUID.randomUUID()); // different owner
    when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

    assertThatThrownBy(() -> service.getDashboard(vendorId, venueId))
        .isInstanceOf(ForbiddenException.class);
    verifyNoInteractions(bookingRepository);
  }

  @Test
  void dashboardRejectsUnknownVenue() {
    when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getDashboard(vendorId, venueId))
        .isInstanceOf(NotFoundException.class);
  }

  // --- schedule window validation -------------------------------------------

  @Test
  void scheduleRejectsInvertedAndOversizedWindows() {
    Venue venue = venue(venueId, vendorId);
    when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

    LocalDateTime now = LocalDateTime.now();

    assertThatThrownBy(() -> service.getSchedule(
        vendorId, venueId, now, now.minusDays(1), null))
        .isInstanceOf(BadRequestException.class);

    assertThatThrownBy(() -> service.getSchedule(
        vendorId, venueId, now, now.plusDays(120), null))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("92");
  }

  // --- no-show rules ----------------------------------------------------------

  @Test
  void markNoShowHappyPathNotifiesPlayerAndTimestamps() {
    Booking booking = confirmedBookingAt(vendorId, LocalDateTime.now().minusHours(2));
    User player = new User();
    player.setId(UUID.randomUUID());
    booking.setUser(player);
    when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

    service.markNoShow(vendorId, UUID.randomUUID());

    assertThat(booking.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
    assertThat(booking.getNoShowMarkedAt()).isNotNull();          // audit trail
    verify(bookingRepository).save(booking);
    // Fairness: the player must be told their reliability was affected.
    verify(notificationService).sendNoShowMarked(eq(player), eq(booking.getId()), any());
  }

  @Test
  void markNoShowRejectsOtherVendorsBooking() {
    Booking booking = confirmedBookingAt(UUID.randomUUID(), LocalDateTime.now().minusHours(2));
    when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> service.markNoShow(vendorId, UUID.randomUUID()))
        .isInstanceOf(ForbiddenException.class);
    verify(bookingRepository, never()).save(any());
  }

  @Test
  void markNoShowRejectsFutureBooking() {
    Booking booking = confirmedBookingAt(vendorId, LocalDateTime.now().plusHours(2));
    when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> service.markNoShow(vendorId, UUID.randomUUID()))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("future");
  }

  @Test
  void markNoShowRejectsNonConfirmedBooking() {
    Booking booking = confirmedBookingAt(vendorId, LocalDateTime.now().minusHours(2));
    booking.setStatus(BookingStatus.CANCELLED);
    when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

    assertThatThrownBy(() -> service.markNoShow(vendorId, UUID.randomUUID()))
        .isInstanceOf(BadRequestException.class);
  }

  // --- helpers ----------------------------------------------------------------

  private Venue venue(UUID id, UUID ownerId) {
    User owner = new User();
    owner.setId(ownerId);
    Venue v = new Venue();
    v.setId(id);
    v.setName("Test Venue");
    v.setOwner(owner);
    return v;
  }

  private Booking confirmedBookingAt(UUID ownerId, LocalDateTime start) {
    Venue v = venue(UUID.randomUUID(), ownerId);
    Courts court = new Courts();
    court.setVenue(v);
    court.setCourtNumber("Court 1");

    Booking b = new Booking();
    b.setId(UUID.randomUUID());
    b.setCourt(court);
    b.setStatus(BookingStatus.CONFIRMED);
    b.setStartTime(start);
    b.setEndTime(start.plusHours(1));
    return b;
  }
}
