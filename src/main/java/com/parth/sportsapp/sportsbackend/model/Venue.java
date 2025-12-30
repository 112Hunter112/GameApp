package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "venues")
public class Venue {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String address;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(columnDefinition = "TEXT")
  private String description;

  /**
   * Using @JdbcTypeCode(SqlTypes.JSON) allows Hibernate 6 to map this Map
   * directly to a JSONB column in PostgreSQL.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> openingHours;

  /**
   * Maps to the text[] type in your schema.
   */
  @ElementCollection
  @CollectionTable(name = "venue_amenities", joinColumns = @JoinColumn(name = "venue_id"))
  @Column(name = "amenity")
  private List<String> amenities;

  @Column(columnDefinition = "geometry(Point, 4326)")
  private Point location;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL)
  private List<Courts> courts;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  // --- Constructors ---

  public Venue() {}

  // --- Getters and Setters ---

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public User getOwner() { return owner; }
  public void setOwner(User owner) { this.owner = owner; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }

  public String getPhoneNumber() { return phoneNumber; }
  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public Map<String, Object> getOpeningHours() { return openingHours; }
  public void setOpeningHours(Map<String, Object> openingHours) { this.openingHours = openingHours; }

  public List<String> getAmenities() { return amenities; }
  public void setAmenities(List<String> amenities) { this.amenities = amenities; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

  public List<Courts> getCourts() {
    return courts;
  }

  public void setCourts(List<Courts> courts) {
    this.courts = courts;
  }

  // Helper methods to make it easy to work with
  public Double getLatitude() {
    return location != null ? location.getY() : null;
  }

  public Double getLongitude() {
    return location != null ? location.getX() : null;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public Point getLocation() {
    return location;
  }

  public void setLocation(Point location) {
    this.location = location;
  }
}
