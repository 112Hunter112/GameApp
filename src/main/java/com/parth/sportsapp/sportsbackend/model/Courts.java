package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
public class Courts {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "venue_id", nullable = false)
  private Venue venue;

  @ManyToOne
  @JoinColumn(name = "sports_id", nullable = false)
  private Sports sports;

@Column(name = "court_number")
  private String courtNumber;

  @Column(name = "hourly_rate")
  private BigDecimal hourlyRate; // Use BigDecimal for currency precision

  @Column(name = "is_indoor")
  private boolean isIndoor;

  @Column(name = "surface_type")
  private String surfaceType; // e.g., Wood, Grass, Turf

  private Integer capacity;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "court_amenities", joinColumns = @JoinColumn(name = "court_id"))
  @Column(name = "amenity")
  private List<String> amenities;

  @Column(name = "is_active")
  private boolean isActive = true;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "court")
  private List<Booking> bookings;

  public Courts() {}


  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Venue getVenue() {
    return venue;
  }

  public void setVenue(Venue venue) {
    this.venue = venue;
  }

  public Sports getSports() {
    return sports;
  }

  public void setSports(Sports sports) {
    this.sports = sports;
  }

  public String getCourtNumber() {
    return courtNumber;
  }

  public void setCourtNumber(String courtNumber) {
    this.courtNumber = courtNumber;
  }

  public BigDecimal getHourlyRate() {
    return hourlyRate;
  }

  public void setHourlyRate(BigDecimal hourlyRate) {
    this.hourlyRate = hourlyRate;
  }

  public boolean isIndoor() {
    return isIndoor;
  }

  public void setIndoor(boolean indoor) {
    isIndoor = indoor;
  }

  public String getSurfaceType() {
    return surfaceType;
  }

  public void setSurfaceType(String surfaceType) {
    this.surfaceType = surfaceType;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public List<String> getAmenities() {
    return amenities;
  }

  public void setAmenities(List<String> amenities) {
    this.amenities = amenities;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public List<Booking> getBookings() {
    return bookings;
  }

  public void setBookings(List<Booking> bookings) {
    this.bookings = bookings;
  }
}
