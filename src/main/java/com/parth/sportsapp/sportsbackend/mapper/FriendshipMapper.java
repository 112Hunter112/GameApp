package com.parth.sportsapp.sportsbackend.mapper;


import com.parth.sportsapp.sportsbackend.dto.FriendshipRequest;
import com.parth.sportsapp.sportsbackend.dto.FriendshipResponse;
import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.model.Friendship;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.model.Venue;
import org.springframework.stereotype.Component;

@Component
public class FriendshipMapper {

  // Logic you need to write:
  public FriendshipResponse toResponse(Friendship friendship) {
    // 1. Create new Response DTO
    FriendshipResponse friendshipResponse = new FriendshipResponse();

    // 2. Map ID, Status, CreatedAt
    friendshipResponse.setId(friendship.getId());
    friendshipResponse.setStatus(friendship.getStatus());
    friendshipResponse.setCreatedAt(friendship.getCreatedAt());

    // 3. Convert friendship.getRequester() -> UserSummaryDto
UserSummaryDto requester = new UserSummaryDto();
requester.setId(friendship.getRequester().getId());
requester.setFirstName(friendship.getRequester().getFirstName());
requester.setLastName(friendship.getRequester().getLastName());
requester.setEmail(friendship.getRequester().getEmail());
friendshipResponse.setRequester(requester);

    // 4. Convert friendship.getReceiver() -> UserSummaryDto
    UserSummaryDto receiver = new UserSummaryDto();
    receiver.setId(friendship.getReceiver().getId());
    receiver.setFirstName(friendship.getReceiver().getFirstName());
    receiver.setLastName(friendship.getReceiver().getLastName());
    receiver.setEmail(friendship.getReceiver().getEmail());
    friendshipResponse.setReceiver(receiver);
    // 5. Return Response
    return friendshipResponse;
  }
}
