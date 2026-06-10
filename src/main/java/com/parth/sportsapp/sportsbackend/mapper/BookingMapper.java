package com.parth.sportsapp.sportsbackend.mapper;

import com.parth.sportsapp.sportsbackend.dto.BookingResponse;
import com.parth.sportsapp.sportsbackend.model.Booking;
import com.parth.sportsapp.sportsbackend.model.Courts;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class BookingMapper {

  /**
   * @param includePlayer true for owner-facing views (the owner needs to know who booked);
   *                      false for player-facing views (don't echo their own identity).
   */
  public BookingResponse toResponse(Booking booking, boolean includePlayer, boolean canCancel) {
    if (booking == null) return null;

    BookingResponse r = new BookingResponse();
    r.setId(booking.getId());
    r.setStatus(booking.getStatus());

    r.setStartTime(booking.getStartTime());
    r.setEndTime(booking.getEndTime());
    r.setDurationMinutes(
        (int) Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes());

    Courts court = booking.getCourt();
    r.setCourtId(court.getId());
    r.setCourtNumber(court.getCourtNumber());
    r.setSportName(court.getSports() != null ? court.getSports().getSportName() : null);

    Venue venue = court.getVenue();
    r.setVenueId(venue.getId());
    r.setVenueName(venue.getName());
    r.setVenueAddress(venue.getAddress());

    if (includePlayer) {
      User player = booking.getUser();
      r.setPlayerId(player.getId());
      r.setPlayerName(player.getFirstName() + " " + player.getLastName());
    }

    r.setTotalPrice(booking.getTotalPrice());
    r.setPaymentStatus(booking.getPaymentStatus());

    r.setNotes(booking.getNotes());
    r.setCreatedAt(booking.getCreatedAt());
    r.setConfirmedAt(booking.getConfirmedAt());
    r.setCancelledAt(booking.getCancelledAt());
    r.setCancelledBy(booking.getCancelledBy());
    r.setCancellationReason(booking.getCancellationReason());
    r.setCanCancel(canCancel);

    return r;
  }
}
