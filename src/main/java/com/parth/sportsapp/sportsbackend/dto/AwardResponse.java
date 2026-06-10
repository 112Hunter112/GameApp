package com.parth.sportsapp.sportsbackend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * What the API returns for a single Award, including its images.
 */
public class AwardResponse {

    private UUID id;
    private String placementLabel;
    private String competitionName;
    private String location;
    private LocalDate awardDate;
    private String description;

    // Flattened sport fields — no second request needed
    private UUID sportId;
    private String sportName;

    private List<AwardImageResponse> images;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AwardResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public UUID getSportId() { return sportId; }
    public void setSportId(UUID sportId) { this.sportId = sportId; }

    public String getSportName() { return sportName; }
    public void setSportName(String sportName) { this.sportName = sportName; }

    public List<AwardImageResponse> getImages() { return images; }
    public void setImages(List<AwardImageResponse> images) { this.images = images; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
