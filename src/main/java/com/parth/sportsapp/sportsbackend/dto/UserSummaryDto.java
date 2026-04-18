package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.FriendshipStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserSummaryDto {
  private UUID id;
  private String firstName;
  private String lastName;
  private String username;
  private String email;
  private String bio;
  private String profilePictureUrl;

  public UserSummaryDto() {}

  public UserSummaryDto(UUID id, String firstName, String lastName, String username, String email, String bio, String profilePictureUrl) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.username = username;
    this.email = email;
    this.bio = bio;
    this.profilePictureUrl = profilePictureUrl;
  }

  // ... generate getters ...

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public String getProfilePictureUrl() {
    return profilePictureUrl;
  }

  public void setProfilePictureUrl(String profilePictureUrl) {
    this.profilePictureUrl = profilePictureUrl;
  }
}
