package com.parth.sportsapp.sportsbackend.mapper;

import com.parth.sportsapp.sportsbackend.dto.MatchResponse;
import com.parth.sportsapp.sportsbackend.dto.ParticipantDto;
import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.model.Match;
import com.parth.sportsapp.sportsbackend.model.Participants;
import com.parth.sportsapp.sportsbackend.model.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MatchMapper {

  public MatchResponse toDto(Match match) {
    if (match == null) {
      return null;
    }

    MatchResponse response = new MatchResponse();

    // 1. Map Basic Fields
    response.setId(match.getId());
    response.setScore(match.getScore());
    response.setDate(match.getMatchDate());
    response.setSource(match.getSource());
    response.setVerificationStatus(match.getVerificationStatus());
    response.setNotes(match.getDescription());
    response.setExternalOpponentName(match.getExternalOpponentName());

    // 2. Map User Objects (using helper method)
    if (match.getCreatedByUser() != null) {
      response.setCreatedBy(toUserSummary(match.getCreatedByUser()));
    }

    response.setWinningTeam(match.getWinningTeam());

    // 3. Map Linked Booking ID
    if (match.getBooking() != null) {
      response.setLinkedBookingId(match.getBooking().getId());
    }

    // 4. Map Participants List
    if (match.getParticipants() != null && !match.getParticipants().isEmpty()) {
      List<ParticipantDto> participantDtos = match.getParticipants().stream()
          .map(this::toParticipantDto) // Calls the helper below
          .collect(Collectors.toList());

      response.setParticipants(participantDtos);
    } else {
      response.setParticipants(Collections.emptyList());
    }

    return response;
  }

  // --- HELPER METHODS ---

  private ParticipantDto toParticipantDto(Participants participant) {
    ParticipantDto dto = new ParticipantDto();

    // Map the User inside the participant
    if (participant.getUser() != null) {
      dto.setUser(toUserSummary(participant.getUser()));
    }

    dto.setHost(participant.isHost());
    dto.setStatus(participant.getStatus().name());

    return dto;
  }

  private UserSummaryDto toUserSummary(User user) {
    UserSummaryDto dto = new UserSummaryDto();
    dto.setId(user.getId());
    dto.setFirstName(user.getFirstName());
    dto.setLastName(user.getLastName());
    dto.setEmail(user.getEmail());
    return dto;
  }
}
