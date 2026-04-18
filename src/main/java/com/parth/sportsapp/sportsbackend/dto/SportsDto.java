package com.parth.sportsapp.sportsbackend.dto;

import java.util.UUID;

public class SportsDto {

    private UUID id;
    private String sportName;
    private String scoringType;

    public SportsDto(UUID id, String sportName, String scoringType) {
        this.id = id;
        this.sportName = sportName;
        this.scoringType = scoringType;
    }

    public UUID getId() { return id; }
    public String getSportName() { return sportName; }
    public String getScoringType() { return scoringType; }
}
