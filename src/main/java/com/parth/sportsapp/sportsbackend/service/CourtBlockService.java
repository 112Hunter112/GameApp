package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.CourtBlockRequest;
import com.parth.sportsapp.sportsbackend.dto.CourtBlockResponse;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.CourtBlock;
import com.parth.sportsapp.sportsbackend.model.Courts;
import com.parth.sportsapp.sportsbackend.repository.CourtBlockRepository;
import com.parth.sportsapp.sportsbackend.repository.CourtRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Owner-managed court holds. A block makes time unavailable to players without
 * touching existing bookings — if a confirmed booking already sits inside the
 * blocked window, the owner cancels it separately (and the player is notified).
 */
@Service
@Transactional
public class CourtBlockService {

  @Autowired private CourtBlockRepository courtBlockRepository;
  @Autowired private CourtRepository courtRepository;

  public CourtBlockResponse createBlock(UUID courtId, UUID ownerId, CourtBlockRequest request) {
    // Same serialization point as BookingService.createBooking: hold the court
    // row lock across the overlap check, or a block and a booking (or two
    // blocks) can both pass their checks concurrently and land on one window.
    Courts court = courtRepository.findByIdForUpdate(courtId)
        .orElseThrow(() -> new NotFoundException("Court not found"));
    if (!court.getVenue().getOwner().getId().equals(ownerId)) {
      throw new ForbiddenException("You do not own this court");
    }

    LocalDateTime start = request.getStartTime();
    LocalDateTime end = request.getEndTime();
    if (start == null || end == null || !end.isAfter(start)) {
      throw new BadRequestException("End time must be after start time");
    }
    if (end.isAfter(start.plusDays(31))) {
      throw new BadRequestException("A single block cannot exceed 31 days");
    }
    if (courtBlockRepository.countOverlapping(courtId, start, end) > 0) {
      throw new BadRequestException("This window overlaps an existing block");
    }

    CourtBlock block = new CourtBlock();
    block.setCourt(court);
    block.setStartTime(start);
    block.setEndTime(end);
    block.setReason(request.getReason() == null || request.getReason().isBlank()
        ? null : request.getReason().trim());

    return toResponse(courtBlockRepository.save(block));
  }

  public void deleteBlock(UUID blockId, UUID ownerId) {
    CourtBlock block = courtBlockRepository.findById(blockId)
        .orElseThrow(() -> new NotFoundException("Block not found"));
    if (!block.getCourt().getVenue().getOwner().getId().equals(ownerId)) {
      throw new ForbiddenException("You do not own this block");
    }
    courtBlockRepository.delete(block);
  }

  /** All of an owner's blocks touching one day (optionally one venue) — host calendar feed. */
  @Transactional(readOnly = true)
  public List<CourtBlockResponse> getOwnerBlocks(UUID ownerId, UUID venueId, LocalDate date) {
    LocalDateTime dayStart = date.atStartOfDay();
    LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
    return courtBlockRepository.findForOwner(ownerId, venueId, dayStart, dayEnd)
        .stream().map(this::toResponse).toList();
  }

  private CourtBlockResponse toResponse(CourtBlock b) {
    CourtBlockResponse r = new CourtBlockResponse();
    r.setId(b.getId());
    r.setCourtId(b.getCourt().getId());
    r.setCourtNumber(b.getCourt().getCourtNumber());
    r.setVenueId(b.getCourt().getVenue().getId());
    r.setVenueName(b.getCourt().getVenue().getName());
    r.setStartTime(b.getStartTime());
    r.setEndTime(b.getEndTime());
    r.setReason(b.getReason());
    return r;
  }
}
