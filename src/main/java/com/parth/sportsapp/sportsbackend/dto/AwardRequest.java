package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Body for POST /api/me/awards and PUT /api/me/awards/{id}.
 */
public class AwardRequest {

    /**
     * Short label shown on the trophy card.
     * Examples: "1st Place", "2nd Place", "3rd Place", "MVP", "Winner", "Finalist"
     */
    @NotBlank(message = "placementLabel is required")
    @Size(max = 50, message = "placementLabel must be 50 characters or fewer")
    private String placementLabel;

    /** Name of the competition or event. */
    @NotBlank(message = "competitionName is required")
    @Size(max = 200, message = "competitionName must be 200 characters or fewer")
    private String competitionName;

    /** Optional city or venue. */
    @Size(max = 200, message = "location must be 200 characters or fewer")
    private String location;

    /** Date the award was received. Cannot be in the future. */
    @NotNull(message = "awardDate is required")
    @PastOrPresent(message = "awardDate cannot be in the future")
    private LocalDate awardDate;

    /** Optional description. e.g. "Beat 340 competitors, personal best time." */
    @Size(max = 1000, message = "description must be 1000 characters or fewer")
    private String description;

    /** Optional sport this award is associated with. */
    private UUID sportId;

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
}
