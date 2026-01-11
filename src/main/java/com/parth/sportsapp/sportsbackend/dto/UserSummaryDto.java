package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.FriendshipStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserSummaryDto {
  private UUID id;
  private String firstName;
  private String lastName;
  private String email;
  private String bio;
  private String profilePictureUrl;
  // private String avatarUrl; (Add this later if you have images)

public UserSummaryDto() {}

  // Constructors, Getters & Setters
  public UserSummaryDto(UUID id, String firstName, String lastName, String email, String bio, String profilePictureUrl) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
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
