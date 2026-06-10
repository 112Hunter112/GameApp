package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.AwardImageRequest;
import com.parth.sportsapp.sportsbackend.dto.AwardImageResponse;
import com.parth.sportsapp.sportsbackend.dto.AwardRequest;
import com.parth.sportsapp.sportsbackend.dto.AwardResponse;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.Award;
import com.parth.sportsapp.sportsbackend.model.AwardImage;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.AwardImageRepository;
import com.parth.sportsapp.sportsbackend.repository.AwardRepository;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AwardService {

    /** Maximum photos per award — prevents storage abuse. */
    private static final int MAX_IMAGES_PER_AWARD = 10;

    private final AwardRepository awardRepository;
    private final AwardImageRepository awardImageRepository;
    private final UserRepository userRepository;
    private final SportsRepository sportsRepository;

    public AwardService(AwardRepository awardRepository,
                        AwardImageRepository awardImageRepository,
                        UserRepository userRepository,
                        SportsRepository sportsRepository) {
        this.awardRepository = awardRepository;
        this.awardImageRepository = awardImageRepository;
        this.userRepository = userRepository;
        this.sportsRepository = sportsRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Awards CRUD
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AwardResponse create(UUID userId, AwardRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Award award = new Award();
        award.setUser(user);
        applyRequest(award, request);

        return toResponse(awardRepository.save(award));
    }

    @Transactional(readOnly = true)
    public List<AwardResponse> listForUser(UUID userId) {
        return awardRepository.findByUserIdWithImages(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AwardResponse get(UUID awardId, UUID requestingUserId) {
        Award award = findAward(awardId);
        // Awards are public — anyone authenticated can view them
        return toResponse(award);
    }

    @Transactional
    public AwardResponse update(UUID awardId, UUID userId, AwardRequest request) {
        Award award = findAward(awardId);
        requireOwner(award, userId);
        applyRequest(award, request);
        return toResponse(awardRepository.save(award));
    }

    @Transactional
    public void delete(UUID awardId, UUID userId) {
        Award award = findAward(awardId);
        requireOwner(award, userId);
        awardRepository.delete(award);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Image management
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AwardImageResponse addImage(UUID awardId, UUID userId, AwardImageRequest request) {
        Award award = findAward(awardId);
        requireOwner(award, userId);

        long existing = awardImageRepository.countByAwardId(awardId);
        if (existing >= MAX_IMAGES_PER_AWARD) {
            throw new BadRequestException(
                "Maximum of " + MAX_IMAGES_PER_AWARD + " photos per award reached");
        }

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        AwardImage image = new AwardImage();
        image.setAward(award);
        image.setUploadedBy(uploader);
        image.setImageUrl(request.getImageUrl());
        image.setCaption(request.getCaption());

        return toImageResponse(awardImageRepository.save(image));
    }

    @Transactional(readOnly = true)
    public List<AwardImageResponse> listImages(UUID awardId, UUID requestingUserId) {
        // Verify the award exists (throws NotFoundException if not)
        findAward(awardId);
        return awardImageRepository.findAll()
                .stream()
                .filter(img -> img.getAward().getId().equals(awardId))
                .map(this::toImageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteImage(UUID awardId, UUID imageId, UUID userId) {
        AwardImage image = awardImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("Image not found"));

        if (!image.getAward().getId().equals(awardId)) {
            throw new NotFoundException("Image not found on this award");
        }
        if (!image.getUploadedBy().getId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own photos");
        }

        awardImageRepository.delete(image);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Award findAward(UUID awardId) {
        return awardRepository.findById(awardId)
                .orElseThrow(() -> new NotFoundException("Award not found"));
    }

    private void requireOwner(Award award, UUID userId) {
        if (!award.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only modify your own awards");
        }
    }

    private void applyRequest(Award award, AwardRequest request) {
        award.setPlacementLabel(request.getPlacementLabel());
        award.setCompetitionName(request.getCompetitionName());
        award.setLocation(request.getLocation());
        award.setAwardDate(request.getAwardDate());
        award.setDescription(request.getDescription());

        if (request.getSportId() != null) {
            Sports sport = sportsRepository.findById(request.getSportId())
                    .orElseThrow(() -> new NotFoundException("Sport not found"));
            award.setSport(sport);
        } else {
            award.setSport(null);
        }
    }

    private AwardResponse toResponse(Award award) {
        AwardResponse r = new AwardResponse();
        r.setId(award.getId());
        r.setPlacementLabel(award.getPlacementLabel());
        r.setCompetitionName(award.getCompetitionName());
        r.setLocation(award.getLocation());
        r.setAwardDate(award.getAwardDate());
        r.setDescription(award.getDescription());
        r.setCreatedAt(award.getCreatedAt());
        r.setUpdatedAt(award.getUpdatedAt());

        if (award.getSport() != null) {
            r.setSportId(award.getSport().getId());
            r.setSportName(award.getSport().getSportName());
        }

        r.setImages(
            award.getImages().stream()
                .map(this::toImageResponse)
                .collect(Collectors.toList())
        );

        return r;
    }

    private AwardImageResponse toImageResponse(AwardImage image) {
        AwardImageResponse r = new AwardImageResponse();
        r.setId(image.getId());
        r.setImageUrl(image.getImageUrl());
        r.setCaption(image.getCaption());
        r.setUploadedByUserId(image.getUploadedBy().getId());
        r.setCreatedAt(image.getCreatedAt());
        return r;
    }
}
