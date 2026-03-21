package com.parth.sportsapp.sportsbackend.dto;

import java.util.List;

public class PublicProfileResponse {

    private UserSummaryDto profile;
    private UserStatsDto stats;
    private StreakDto streak;
    private List<MatchResponse> recentMatches;

    public PublicProfileResponse(UserSummaryDto profile, UserStatsDto stats, StreakDto streak, List<MatchResponse> recentMatches) {
        this.profile = profile;
        this.stats = stats;
        this.streak = streak;
        this.recentMatches = recentMatches;
    }

    public UserSummaryDto getProfile() { return profile; }
    public UserStatsDto getStats() { return stats; }
    public StreakDto getStreak() { return streak; }
    public List<MatchResponse> getRecentMatches() { return recentMatches; }
}
