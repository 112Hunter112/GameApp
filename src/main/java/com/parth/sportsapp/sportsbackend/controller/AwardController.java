package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.AwardImageRequest;
import com.parth.sportsapp.sportsbackend.dto.AwardImageResponse;
import com.parth.sportsapp.sportsbackend.dto.AwardRequest;
import com.parth.sportsapp.sportsbackend.dto.AwardResponse;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.AwardService;
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
 * Trophy cabinet — standalone awards and their photos.
 *
 * My awards:    /api/me/awards
 * Public view:  /api/users/{userId}/awards  (GET only)
 * Photos:       /api/me/awards/{awardId}/images
 */
@RestController
@Validated
@PreAuthorize("isAuthenticated()")
public class AwardController {

    private final AwardService awardService;

    public AwardController(AwardService awardService) {
        this.awardService = awardService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // My awards
    // ─────────────────────────────────────────────────────────────────────────

    /** Create a new award. */
    @PostMapping("/api/me/awards")
    public ResponseEntity<AwardResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AwardRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(awardService.create(user.getId(), request));
    }

    /** List all my awards, newest first. */
    @GetMapping("/api/me/awards")
    public List<AwardResponse> listMine(@AuthenticationPrincipal User user) {
        return awardService.listForUser(user.getId());
    }

    /** Get a single award by ID. */
    @GetMapping("/api/me/awards/{awardId}")
    public AwardResponse get(
            @AuthenticationPrincipal User user,
            @PathVariable UUID awardId) {
        return awardService.get(awardId, user.getId());
    }

    /** Update an award. Only the owner can do this. */
    @PutMapping("/api/me/awards/{awardId}")
    public AwardResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID awardId,
            @Valid @RequestBody AwardRequest request) {
        return awardService.update(awardId, user.getId(), request);
    }

    /** Delete an award and all its photos. Only the owner can do this. */
    @DeleteMapping("/api/me/awards/{awardId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable UUID awardId) {
        awardService.delete(awardId, user.getId());
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public view — see another user's trophy cabinet
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/users/{userId}/awards")
    public List<AwardResponse> listForUser(@PathVariable UUID userId) {
        return awardService.listForUser(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Award photos
    // ─────────────────────────────────────────────────────────────────────────

    /** Attach a photo URL to an award. */
    @PostMapping("/api/me/awards/{awardId}/images")
    public ResponseEntity<AwardImageResponse> addImage(
            @AuthenticationPrincipal User user,
            @PathVariable UUID awardId,
            @Valid @RequestBody AwardImageRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(awardService.addImage(awardId, user.getId(), request));
    }

    /** List all photos on an award. */
    @GetMapping("/api/me/awards/{awardId}/images")
    public List<AwardImageResponse> listImages(
            @AuthenticationPrincipal User user,
            @PathVariable UUID awardId) {
        return awardService.listImages(awardId, user.getId());
    }

    /** Delete a specific photo. Only the uploader can do this. */
    @DeleteMapping("/api/me/awards/{awardId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal User user,
            @PathVariable UUID awardId,
            @PathVariable UUID imageId) {
        awardService.deleteImage(awardId, imageId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
