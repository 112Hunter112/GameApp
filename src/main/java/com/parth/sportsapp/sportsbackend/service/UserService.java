package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

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
        user.getEmail(),
        user.getBio(),             //
        user.getProfilePictureUrl() //
    );
  }
}
