package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
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

  @NotBlank(message = "no blank password")
  @Column(nullable = false)
  private String password;

  @NotBlank(message = "Phone number required") // Added Java validation
  @Column(nullable = false, unique = true)
  private String phoneNumber;

  @CreationTimestamp
  private LocalDateTime joiningDate;

  @Enumerated(EnumType.STRING) // tells the DB to go through the enumerated class
  @Column(nullable = false)
  private UserRole role = UserRole.USER; // current defaultis USER type, but look to modify this
  // todo: learn to modify this so it takes the VENDER and ADMIN role as well


  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt; // to check if they updated any info for troubleshooting


  @Column(nullable = false)
  private Boolean isVerified = false;  // ← NEW: Email verified?

  private String verificationToken;  // ← NEW: Random token for verification

  private LocalDateTime verificationTokenExpiry;  // ← NEW: Token expires after 24 hours

  @OneToMany(mappedBy = "user")
  private List<UserPreference> preferences;


  @OneToMany(mappedBy = "user")
  private List<Booking> bookings;

  @OneToMany(mappedBy = "createdByUser")
  private List<Match> matches;

  @OneToMany(mappedBy = "owner")
  private List<Venue> ownedVenues;

  @OneToMany(mappedBy = "user")
  private List<Participants> participationHistory;

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
