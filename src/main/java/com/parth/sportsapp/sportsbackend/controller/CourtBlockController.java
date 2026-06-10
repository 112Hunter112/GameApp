package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.CourtBlockRequest;
import com.parth.sportsapp.sportsbackend.dto.CourtBlockResponse;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.CourtBlockService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Court blocks — owner-only holds (maintenance, league nights, private events).
 *
 *   POST   /api/courts/{courtId}/blocks          create a hold
 *   DELETE /api/blocks/{blockId}                  remove a hold
 *   GET    /api/owner/blocks?venueId=&date=       all holds for the host calendar
 */
@RestController
@RequestMapping("/api")
public class CourtBlockController {

  @Autowired
  private CourtBlockService courtBlockService;

  @PostMapping("/courts/{courtId}/blocks")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<CourtBlockResponse> createBlock(
      @AuthenticationPrincipal User user,
      @PathVariable UUID courtId,
      @Valid @RequestBody CourtBlockRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(courtBlockService.createBlock(courtId, user.getId(), request));
  }

  @DeleteMapping("/blocks/{blockId}")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<Void> deleteBlock(
      @AuthenticationPrincipal User user,
      @PathVariable UUID blockId) {
    courtBlockService.deleteBlock(blockId, user.getId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/owner/blocks")
  @PreAuthorize("hasRole('VENUE_OWNER')")
  public ResponseEntity<List<CourtBlockResponse>> getOwnerBlocks(
      @AuthenticationPrincipal User user,
      @RequestParam(name = "venueId", required = false) UUID venueId,
      @RequestParam(name = "date")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ResponseEntity.ok(courtBlockService.getOwnerBlocks(user.getId(), venueId, date));
  }
}
