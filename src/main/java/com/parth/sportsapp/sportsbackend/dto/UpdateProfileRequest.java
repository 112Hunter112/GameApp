package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 50, message = "First name too long")
    private String firstName;
    @Size(max = 50, message = "Last name too long")
    private String lastName;
    @Size(max = 500, message = "Bio too long")
    private String bio;
    @Size(max = 2048, message = "URL too long")
    private String profilePictureUrl;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
}
