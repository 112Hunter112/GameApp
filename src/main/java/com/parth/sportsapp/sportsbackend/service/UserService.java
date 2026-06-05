package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.PublicProfileResponse;
import com.parth.sportsapp.sportsbackend.dto.StreakDto;
import com.parth.sportsapp.sportsbackend.dto.UpdateProfileRequest;
import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private MatchService matchService;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private PwnedPasswordService pwnedPasswordService;

  /**
   * Existing search method - Updated to use the new Constructor
   */
  public List<UserSummaryDto> searchUsers(String query) {
    List<User> users = userRepository.searchUsers(query);

    return users.stream()
        .map(user -> new UserSummaryDto(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getUsername(),
            user.getEmail(),
            user.getBio(),
            user.getProfilePictureUrl()
        ))
        .collect(Collectors.toList());
  }

  /**
   * Fetch the full profile for a specific user ID.
   */
  public UserSummaryDto getUserProfile(UUID userId) {
    // 1. Find the user or throw an error if they don't exist
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

    // 2. Convert to DTO (now including Bio and Avatar)
    return new UserSummaryDto(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getUsername(),
        user.getEmail(),
        user.getBio(),
        user.getProfilePictureUrl()
    );
  }

  /**
   * Update the profile fields for a given user.
   */
  public UserSummaryDto updateProfile(UUID userId, UpdateProfileRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

    if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
      user.setFirstName(request.getFirstName());
    }
    if (request.getLastName() != null && !request.getLastName().isBlank()) {
      user.setLastName(request.getLastName());
    }
    if (request.getBio() != null) {
      user.setBio(request.getBio());
    }
    if (request.getProfilePictureUrl() != null) {
      user.setProfilePictureUrl(request.getProfilePictureUrl());
    }

    userRepository.save(user);

    return new UserSummaryDto(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getUsername(),
        user.getEmail(),
        user.getBio(),
        user.getProfilePictureUrl()
    );
  }

  /**
   * Get a user's full public profile including stats, streak and recent matches.
   */
  public PublicProfileResponse getPublicProfile(UUID targetUserId) {
    User user = userRepository.findById(targetUserId)
        .orElseThrow(() -> new RuntimeException("User not found with ID: " + targetUserId));

    UserSummaryDto profile = new UserSummaryDto(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getUsername(),
        user.getEmail(),
        user.getBio(),
        user.getProfilePictureUrl()
    );

    var stats = matchService.getOverallStats(targetUserId);
    int streakCount = matchService.getCurrentStreak(targetUserId);
    StreakDto streak = new StreakDto(streakCount > 0 ? "WIN" : "NONE", streakCount);
    var recentMatches = matchService.allMatches(targetUserId, PageRequest.of(0, 10)).getContent();

    return new PublicProfileResponse(profile, stats, streak, recentMatches);
  }

  /**
   * Change password for a user after verifying the current password.
   */
  public void changePassword(UUID userId, String currentPassword, String newPassword) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new RuntimeException("Current password is incorrect");
    }
    if (newPassword == null || newPassword.length() < 8) {
      throw new RuntimeException("New password must be at least 8 characters");
    }
    // NIST SP 800-63B: reject breached passwords on change too.
    if (pwnedPasswordService.isBreached(newPassword)) {
      throw new RuntimeException(
          "This password has appeared in a data breach. Please choose a different one.");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  /**
   * Find user ID by email (for authentication)
   */
  public UUID findUserIdByEmail(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found: " + email));
    return user.getId();
  }
}
