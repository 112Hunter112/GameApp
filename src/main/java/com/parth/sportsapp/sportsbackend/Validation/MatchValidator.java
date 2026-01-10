package com.parth.sportsapp.sportsbackend.Validation;


import com.parth.sportsapp.sportsbackend.dto.ManualMatchRequest;
import com.parth.sportsapp.sportsbackend.model.ScoringType;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.model.User;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class MatchValidator {

  // Regex Definitions
  private static final Pattern SETS_PATTERN = Pattern.compile("^([0-9]+-[0-9]+(\\([0-9]+\\))?(, )?)+$"); // "6-4, 6-3"
  private static final Pattern POINTS_PATTERN = Pattern.compile("^\\d+-\\d+$"); // "2-1"
  private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{1,2}:\\d{2}(:\\d{2})?$"); // "10:30" or "1:30:00"


  /**
   * Main Entry Point
   * This coordinates all the checks. Call this from your Service.
   */
  public void validate(ManualMatchRequest request, Sports sportRules, UUID userId) {

    validateNotNull(request);

    // 1. Check Date
    isValidDate(request.getDate());

    // 2. Check Player Counts
    validatePlayerNumber(request, sportRules);
    validateNoDuplicatePlayers(request, userId);

    // 3. Check Score Format
    isValidScore(request.getScore(), sportRules.getScoringType());

    validateWinningTeam(request.getWinningTeam());
  }

  private void validateNotNull(ManualMatchRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Match request cannot be null.");
    }
    if (request.getSport() == null || request.getSport().isBlank()) {
      throw new IllegalArgumentException("Sport must be specified.");
    }
  }




  /**
   *
   * This method checks to see if the date entered is in the past
   * Takes in values such as date to check if past date
   * Return RuntimeException if we have an issue
   *
   */
  private void isValidDate(LocalDateTime date) {
    if (date == null) {
      throw new IllegalArgumentException("Date cannot be empty.");
    }
    if (date.isAfter(LocalDateTime.now())) {
      throw new IllegalArgumentException("You cannot log a match result for a future date.");
    }

  }

  /**
   *
   *
   * @param request
   * @param sportRules Checks if the input form the user for the sport is valid
   */
  private void validatePlayerNumber(ManualMatchRequest request, Sports sportRules) {
    int teamASize = 1 + (request.getTeammateIds() == null ? 0 : request.getTeammateIds().size());
    int teamBSize = (request.getOpponentIds() == null ? 0 : request.getOpponentIds().size())
        + (request.getOpponentEmails() == null ? 0 : request.getOpponentEmails().size());

    int totalPlayers = teamASize + teamBSize;

    // Must have opponents
    if (teamBSize == 0) {
      throw new IllegalArgumentException("You cannot play a match against nobody! Please add an opponent.");
    }

    // Check minimum players
    if (totalPlayers < sportRules.getMinPlayers()) {
      throw new IllegalArgumentException(
          "Not enough players. " + sportRules.getSportName() + " requires at least "
              + sportRules.getMinPlayers() + " players (you have " + totalPlayers + ")."
      );
    }

    // Check maximum players
    if (totalPlayers > sportRules.getMaxPlayers()) {
      throw new IllegalArgumentException(
          "Too many players. " + sportRules.getSportName() + " allows maximum "
              + sportRules.getMaxPlayers() + " players (you have " + totalPlayers + ")."
      );
    }

    // NEW: Check team balance (teams should be equal size for most sports)
    if (teamASize != teamBSize) {
      // For now, just throw error. Later you could add "allowsUnequalTeams" to Sports entity
      throw new IllegalArgumentException(
          "Teams must be equal. Team A has " + teamASize + " players, Team B has " + teamBSize + "."
      );
    }

    // Sanity check: prevent absurdly large teams
    if (teamASize > sportRules.getMaxPlayers() || teamBSize > sportRules.getMaxPlayers()) {
      throw new IllegalArgumentException("Maximum " + sportRules.getMaxPlayers()+ " players per team.");
    }
  }


  private void isValidScore(String score, ScoringType scoringType) {

    if (score == null || score.isBlank()) return; // Allow empty if allowed

    switch (scoringType) {
      case SETS:
        if (!SETS_PATTERN.matcher(score).matches()) {
          throw new IllegalArgumentException("Invalid score format. Tennis format required (e.g., '6-4, 6-3')");
        }
        break;
      case POINTS:
        if (!POINTS_PATTERN.matcher(score).matches()) {
          throw new IllegalArgumentException("Invalid score format. Points format required (e.g., '2-1')");
        }
        break;
      case TIME:
        if (!TIME_PATTERN.matcher(score).matches()) {
          throw new IllegalArgumentException("Invalid time format. Use 'HH:MM:SS' or 'MM:SS'");
        }
        break;
      case NONE:
        // No validation needed for simple Win/Loss
        break;
      default:
        throw new IllegalArgumentException("Unknown scoring type.");
    }
  }


  /**
   * Ensure no user appears twice (can't be on both teams or listed multiple times)
   */
  private void validateNoDuplicatePlayers(ManualMatchRequest request, UUID creatorId) {
    Set<UUID> allPlayers = new HashSet<>();
    allPlayers.add(creatorId);

    // Check teammates
    if (request.getTeammateIds() != null) {
      for (UUID id : request.getTeammateIds()) {
        if (id.equals(creatorId)) {
          throw new IllegalArgumentException("You cannot add yourself as a teammate.");
        }
        if (!allPlayers.add(id)) {
          throw new IllegalArgumentException("Duplicate player detected in teammates.");
        }
      }
    }

    // Check opponents
    if (request.getOpponentIds() != null) {
      for (UUID id : request.getOpponentIds()) {
        if (id.equals(creatorId)) {
          throw new IllegalArgumentException("You cannot play against yourself.");
        }
        if (!allPlayers.add(id)) {
          throw new IllegalArgumentException("Duplicate player detected. A player cannot be on both teams.");
        }
      }
    }

    // Optional: Check email duplicates
    if (request.getOpponentEmails() != null) {
      Set<String> emails = new HashSet<>();
      for (String email : request.getOpponentEmails()) {
        if (!emails.add(email.toLowerCase())) {
          throw new IllegalArgumentException("Duplicate email detected: " + email);
        }
      }
    }
  }


  private void validateWinningTeam(String winningTeam) {
    if (winningTeam == null) return; // Null is fine (match not finished or draw)

    if (!winningTeam.equals("TEAM_A") && !winningTeam.equals("TEAM_B") && !winningTeam.equals("DRAW")) {
      throw new IllegalArgumentException("Invalid winning team. Must be 'TEAM_A', 'TEAM_B', or 'DRAW'.");
    }
  }




  /**
   * If there's a score showing a clear winner, there should be a winner ID
   * Example: "6-4, 6-3" clearly shows someone won
   */
  public void validateScoreWinnerConsistency(String score, UUID winnerId, ScoringType scoringType) {
    if (score == null || score.isBlank()) return;

    // For SET-based sports (Tennis, Volleyball)
    if (scoringType == ScoringType.SETS) {
      // Simple check: if score exists and isn't "0-0", winner should be set
      if (!score.equals("0-0") && winnerId == null) {
        // This is a WARNING, not an error - allow it but maybe log
        // Some matches might be abandoned/retired
      }
    }
  }


}
