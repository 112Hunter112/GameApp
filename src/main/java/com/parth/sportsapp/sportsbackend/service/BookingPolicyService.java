package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.BookingPolicyRequest;
import com.parth.sportsapp.sportsbackend.dto.BookingPolicyResponse;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.BookingPolicy;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.repository.BookingPolicyRepository;
import com.parth.sportsapp.sportsbackend.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class BookingPolicyService {

  private static final Set<Integer> ALLOWED_INCREMENTS = Set.of(30, 60);
  private static final Pattern HOURS_PATTERN =
      Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$");

  @Autowired
  private BookingPolicyRepository bookingPolicyRepository;

  @Autowired
  private VenueRepository venueRepository;

  /** The effective policy for a venue: the saved one, or platform defaults. Public. */
  @Transactional(readOnly = true)
  public BookingPolicyResponse getPolicy(UUID venueId) {
    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new NotFoundException("Venue not found"));

    return bookingPolicyRepository.findByVenue_Id(venueId)
        .map(p -> toResponse(p, true))
        .orElseGet(() -> toResponse(BookingPolicy.defaults(venue), false));
  }

  /** Same as {@link #getPolicy} but returns the entity, for BookingService's internal use. */
  @Transactional(readOnly = true)
  public BookingPolicy getEffectivePolicy(Venue venue) {
    return bookingPolicyRepository.findByVenue_Id(venue.getId())
        .orElseGet(() -> BookingPolicy.defaults(venue));
  }

  /** Owner-only upsert of their venue's booking rules. */
  public BookingPolicyResponse updatePolicy(UUID venueId, UUID ownerId, BookingPolicyRequest request) {
    Venue venue = venueRepository.findById(venueId)
        .orElseThrow(() -> new NotFoundException("Venue not found"));

    if (!venue.getOwner().getId().equals(ownerId)) {
      throw new ForbiddenException("You do not own this venue");
    }

    validate(request);

    BookingPolicy policy = bookingPolicyRepository.findByVenue_Id(venueId)
        .orElseGet(() -> {
          BookingPolicy p = new BookingPolicy();
          p.setVenue(venue);
          return p;
        });

    policy.setSlotIncrementMinutes(request.getSlotIncrementMinutes());
    policy.setMinBookingMinutes(request.getMinBookingMinutes());
    policy.setMaxBookingMinutes(request.getMaxBookingMinutes());
    policy.setAdvanceBookingDays(request.getAdvanceBookingDays());
    policy.setMinNoticeMinutes(request.getMinNoticeMinutes());
    policy.setCancellationCutoffMinutes(request.getCancellationCutoffMinutes());
    policy.setAutoConfirm(request.getAutoConfirm());
    policy.setOpeningHours(request.getOpeningHours());

    return toResponse(bookingPolicyRepository.save(policy), true);
  }

  // --- validation -------------------------------------------------------------

  private void validate(BookingPolicyRequest r) {
    if (!ALLOWED_INCREMENTS.contains(r.getSlotIncrementMinutes())) {
      throw new BadRequestException("Slot increment must be 30 or 60 minutes");
    }
    int inc = r.getSlotIncrementMinutes();

    if (r.getMinBookingMinutes() % inc != 0 || r.getMaxBookingMinutes() % inc != 0) {
      throw new BadRequestException("Min/max booking length must be a multiple of the slot increment");
    }
    if (r.getMinBookingMinutes() > r.getMaxBookingMinutes()) {
      throw new BadRequestException("Minimum booking length cannot exceed the maximum");
    }

    Map<String, String> hours = r.getOpeningHours();
    if (hours.isEmpty()) {
      throw new BadRequestException("At least one day must be open");
    }
    for (Map.Entry<String, String> e : hours.entrySet()) {
      // Key must be a real day name
      try {
        DayOfWeek.valueOf(e.getKey());
      } catch (IllegalArgumentException ex) {
        throw new BadRequestException("Unknown day: " + e.getKey());
      }
      // Value must be HH:mm-HH:mm with close after open
      String v = e.getValue();
      if (v == null || !HOURS_PATTERN.matcher(v).matches()) {
        throw new BadRequestException("Hours for " + e.getKey() + " must look like 08:00-22:00");
      }
      try {
        LocalTime open = LocalTime.parse(v.substring(0, 5));
        LocalTime close = LocalTime.parse(v.substring(6));
        if (!close.isAfter(open)) {
          throw new BadRequestException("Closing time must be after opening time on " + e.getKey());
        }
      } catch (DateTimeParseException ex) {
        throw new BadRequestException("Hours for " + e.getKey() + " must look like 08:00-22:00");
      }
    }
  }

  private BookingPolicyResponse toResponse(BookingPolicy p, boolean customized) {
    BookingPolicyResponse r = new BookingPolicyResponse();
    r.setVenueId(p.getVenue().getId());
    r.setSlotIncrementMinutes(p.getSlotIncrementMinutes());
    r.setMinBookingMinutes(p.getMinBookingMinutes());
    r.setMaxBookingMinutes(p.getMaxBookingMinutes());
    r.setAdvanceBookingDays(p.getAdvanceBookingDays());
    r.setMinNoticeMinutes(p.getMinNoticeMinutes());
    r.setCancellationCutoffMinutes(p.getCancellationCutoffMinutes());
    r.setAutoConfirm(p.isAutoConfirm());
    r.setOpeningHours(p.getOpeningHours());
    r.setCustomized(customized);
    return r;
  }
}
