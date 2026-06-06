package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.MatchImageRequest;
import com.parth.sportsapp.sportsbackend.dto.MatchImageResponse;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.MatchImageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Match-history image attachments.
 *
 * <p>Endpoints are scoped to a specific match ({@code /api/matches/{matchId}/images})
 * and require a JWT. Ownership / participation checks happen in the service.</p>
 */
@RestController
@RequestMapping("/api/matches/{matchId}/images")
@Validated
@PreAuthorize("isAuthenticated()")
public class MatchImageController {

  private final MatchImageService imageService;

  public MatchImageController(MatchImageService imageService) {
    this.imageService = imageService;
  }

  /** Attach a new image URL to a match. Caller must be a participant. */
  @PostMapping
  public ResponseEntity<MatchImageResponse> add(
      @PathVariable UUID matchId,
      @AuthenticationPrincipal User user,
      @Valid @RequestBody MatchImageRequest request) {
    MatchImageResponse created = imageService.addImage(matchId, user.getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /** List all images on a match, newest first. Caller must be a participant. */
  @GetMapping
  public List<MatchImageResponse> list(
      @PathVariable UUID matchId,
      @AuthenticationPrincipal User user) {
    return imageService.listImages(matchId, user.getId());
  }

  /** Delete an image. Only the original uploader can remove it. */
  @DeleteMapping("/{imageId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID matchId,
      @PathVariable UUID imageId,
      @AuthenticationPrincipal User user) {
    imageService.deleteImage(imageId, user.getId());
    return ResponseEntity.noContent().build();
  }
}
