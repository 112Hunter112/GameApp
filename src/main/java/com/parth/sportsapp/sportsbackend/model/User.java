package com.parth.sportsapp.sportsbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID) // UUID used as DB handles the
  // generation of index for the primary key
  private UUID id; // USE UUID to generate a random ID for each user rather than 1,2,3, ...

  @NotBlank(message = "First name required") // Added Java validation
  @Column(nullable = false)
  private String firstName;


  @NotBlank(message = "Last name required") // Added Java validation
  @Column(nullable = false)
  private String lastName;

  @NotBlank(message = "Email required")
  @Column(nullable = false, unique = true)
  private String email;

  // WRITE_ONLY: can be set when deserializing but is NEVER serialized into a
  // JSON response. Defends against accidental entity exposure leaking the hash.
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @NotBlank(message = "no blank password")
  @Column(nullable = false)
  private String password;

  @Column(unique = true)
  private String phoneNumber;

  @CreationTimestamp
  private LocalDateTime joiningDate;

  @Enumerated(EnumType.STRING) // tells the DB to go through the enumerated class
  @Column(nullable = false)
  private UserRole role = UserRole.USER; // current defaultis USER type, but look to modify this


  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt; // to check if they updated any info for troubleshooting


  @Column(nullable = false)
  private Boolean isVerified = false;  // ← NEW: Email verified?

  // Account-access secrets — never serialize to clients.
  @JsonIgnore
  private String verificationToken;  // ← NEW: Random token for verification

  @JsonIgnore
  private LocalDateTime verificationTokenExpiry;  // ← NEW: Token expires after 24 hours

  @OneToMany(mappedBy = "user")
  private List<UserPreference> preferences = new ArrayList<>();


  @OneToMany(mappedBy = "user")
  private List<Booking> bookings;

  @OneToMany(mappedBy = "createdByUser")
  private List<Match> matches;

  @OneToMany(mappedBy = "owner")
  private List<Venue> ownedVenues;

  @OneToMany(mappedBy = "user")
  private List<Participants> participationHistory;


  @OneToMany(mappedBy = "requester", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Friendship> sentRequests;

  @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Friendship> receivedRequests;

  @Column(unique = true, nullable = true)
  private String username;

  @Column(length = 500)
  private String bio; // "I love tennis and play on weekends!"

  private String profilePictureUrl; // Store the S3 or Cloudinary URL here

  @Enumerated(EnumType.STRING)
  private Gender gender; // Create an Enum: MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY

  private LocalDate dateOfBirth; // Better than storing "int age" because age changes every year

  @JsonIgnore
  private String expoPushToken; // The ID of their phone (for React Native notifications)

  @Column(nullable = false)
  private boolean emailNotificationsEnabled = true;

  @Column(nullable = false)
  private boolean pushNotificationsEnabled = true;

  /** Google's stable user identifier (the "sub" claim). Null if user signed up with email. */
  @JsonIgnore
  @Column(name = "google_id", unique = true)
  private String googleId;

  /**
   * Coarse last-known location, refreshed when the user loads their home feed.
   * Powers Smart Fill (offering empty court slots to nearby players). Never
   * serialized to other clients, never used for anything user-facing beyond
   * the user's own offers. Null until the user first opens the home screen.
   */
  @JsonIgnore
  @Column(name = "last_known_location", columnDefinition = "geometry(Point, 4326)")
  private org.locationtech.jts.geom.Point lastKnownLocation;

  // 1. Existing mapping (Keep this)
  // Maps to: private User createdByUser; in Match.java
  @OneToMany(mappedBy = "createdByUser", fetch = FetchType.LAZY)
  private List<Match> createdMatches = new ArrayList<>();





  /**
   * These are all the getters and setters
   * @return the value they are assigned or set those values in db.
   */

  public String getFirstName() {return firstName;}
  public void setFirstName(String firstName) {this.firstName = firstName;}

  public String getLastName() {return lastName;}
  public void setLastName(String lastName) {this.lastName = lastName;}

  public String getEmail() {return email;}
  public void setEmail(String email) {this.email = email;}

  public String getPassword() {return password;}
  public void setPassword(String password) {this.password = password;}

  public String getPhoneNumber() {return phoneNumber;}
  public void setPhoneNumber(String phoneNumber) {this.phoneNumber = phoneNumber;}

  public LocalDateTime getJoiningDate() {return joiningDate;}
  public void setJoiningDate(LocalDateTime joiningDate) {this.joiningDate = joiningDate;}

  public UUID getId() {return id;}
  public void setId(UUID id) {this.id = id;}

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public Boolean getVerified() {
    return isVerified;
  }

  public void setVerified(Boolean verified) {
    isVerified = verified;
  }

  public String getVerificationToken() {
    return verificationToken;
  }

  public void setVerificationToken(String verificationToken) {
    this.verificationToken = verificationToken;
  }

  public LocalDateTime getVerificationTokenExpiry() {
    return verificationTokenExpiry;
  }

  public void setVerificationTokenExpiry(LocalDateTime verificationTokenExpiry) {
    this.verificationTokenExpiry = verificationTokenExpiry;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<UserPreference> getPreferences() {
    return preferences;
  }

  public void setPreferences(List<UserPreference> preferences) {
    this.preferences = preferences;
  }

  public List<Booking> getBookings() {
    return bookings;
  }

  public void setBookings(List<Booking> bookings) {
    this.bookings = bookings;
  }

  public List<Match> getMatches() {
    return matches;
  }

  public void setMatches(List<Match> matches) {
    this.matches = matches;
  }

  public List<Venue> getOwnedVenues() {
    return ownedVenues;
  }

  public void setOwnedVenues(List<Venue> ownedVenues) {
    this.ownedVenues = ownedVenues;
  }

  public List<Participants> getParticipationHistory() {
    return participationHistory;
  }

  public void setParticipationHistory(List<Participants> participationHistory) {
    this.participationHistory = participationHistory;
  }

  public List<Friendship> getSentRequests() {
    return sentRequests;
  }

  public void setSentRequests(List<Friendship> sentRequests) {
    this.sentRequests = sentRequests;
  }

  public List<Friendship> getReceivedRequests() {
    return receivedRequests;
  }

  public void setReceivedRequests(List<Friendship> receivedRequests) {
    this.receivedRequests = receivedRequests;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
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

  public Gender getGender() {
    return gender;
  }

  public void setGender(Gender gender) {
    this.gender = gender;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public String getExpoPushToken() {
    return expoPushToken;
  }

  public void setExpoPushToken(String expoPushToken) {
    this.expoPushToken = expoPushToken;
  }

  public String getGoogleId() {
    return googleId;
  }

  public void setGoogleId(String googleId) {
    this.googleId = googleId;
  }

  public org.locationtech.jts.geom.Point getLastKnownLocation() {
    return lastKnownLocation;
  }

  public void setLastKnownLocation(org.locationtech.jts.geom.Point lastKnownLocation) {
    this.lastKnownLocation = lastKnownLocation;
  }

  public boolean isEmailNotificationsEnabled() {
    return emailNotificationsEnabled;
  }

  public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) {
    this.emailNotificationsEnabled = emailNotificationsEnabled;
  }

  public boolean isPushNotificationsEnabled() {
    return pushNotificationsEnabled;
  }

  public void setPushNotificationsEnabled(boolean pushNotificationsEnabled) {
    this.pushNotificationsEnabled = pushNotificationsEnabled;
  }

  public List<Match> getCreatedMatches() {
    return createdMatches;
  }

  public void setCreatedMatches(List<Match> createdMatches) {
    this.createdMatches = createdMatches;
  }



  //  default constructor
  public User() {

  }

  // Logic: Only ask for what the human user actually types in
  public User(String firstName, String lastName, String email, String password, String phoneNumber) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.password = password;
    this.phoneNumber = phoneNumber;
    // We leave ID, role, and dates alone.
    // Java sets them to null/default, and then Hibernate/DB fills them in automatically.
  }
}
