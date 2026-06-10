package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A real-world award, trophy, or placement that a user wants to showcase
 * on their profile. Examples: "1st Place – John's Marathon, NJ" or
 * "MVP – City Basketball League Spring 2024".
 *
 * Awards are standalone — not tied to a logged match — so users can add
 * tournament wins, race placements, and competition medals even if they
 * didn't track the event inside the app.
 */
@Entity
@Table(name = "awards", indexes = {
    @Index(name = "idx_award_user", columnList = "user_id"),
    @Index(name = "idx_award_date",  columnList = "award_date")
})
public class Award {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The user this award belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Optional sport link — e.g. Tennis, Running. Null if the award is
     * not sport-specific (e.g. "Best Team Spirit").
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id")
    private Sports sport;

    /**
     * Short placement label shown on the card.
     * Standard values: "1st Place", "2nd Place", "3rd Place".
     * Custom examples: "MVP", "Winner", "Finalist", "Best Time".
     * Max 50 chars.
     */
    @Column(name = "placement_label", nullable = false, length = 50)
    private String placementLabel;

    /** Name of the competition / event. e.g. "John's Marathon" */
    @Column(name = "competition_name", nullable = false, length = 200)
    private String competitionName;

    /** Optional city / venue. e.g. "New Jersey" or "Madison Square Garden" */
    @Column(name = "location", length = 200)
    private String location;

    /** Date the award was received / competition was held. */
    @Column(name = "award_date", nullable = false)
    private LocalDate awardDate;

    /**
     * Optional short description. e.g. "Beat 340 competitors, personal best time."
     * Max 1000 chars.
     */
    @Column(name = "description", length = 1000)
    private String description;

    /** Photos attached to this award (podium shots, finish-line photos, etc.) */
    @OneToMany(mappedBy = "award", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<AwardImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Award() {}

    // Getters & setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Sports getSport() { return sport; }
    public void setSport(Sports sport) { this.sport = sport; }

    public String getPlacementLabel() { return placementLabel; }
    public void setPlacementLabel(String placementLabel) { this.placementLabel = placementLabel; }

    public String getCompetitionName() { return competitionName; }
    public void setCompetitionName(String competitionName) { this.competitionName = competitionName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getAwardDate() { return awardDate; }
    public void setAwardDate(LocalDate awardDate) { this.awardDate = awardDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<AwardImage> getImages() { return images; }
    public void setImages(List<AwardImage> images) { this.images = images; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
