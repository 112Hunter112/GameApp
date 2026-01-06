package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.FriendshipResponse;
import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.mapper.FriendshipMapper;
import com.parth.sportsapp.sportsbackend.model.Friendship;
import com.parth.sportsapp.sportsbackend.model.FriendshipStatus;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.FriendshipRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FriendshipService {


  @Autowired
  private UserRepository userRepository;

  @Autowired
  private FriendshipRepository friendshipRepository;

  @Autowired
  private FriendshipMapper friendshipMapper;


  // a person should be able to send a friend request, if a person tries to re-register the
  // request using the url, call existsFriendshipBetween and see if false or not
  /**
   * Sends a friend request.
   * @param requesterId - The ID of the person currently logged in (From JWT)
   * @param receiverId - The ID of the person they want to friend (From DTO)
   */
  public FriendshipResponse friendRequest(UUID requesterId, UUID receiverId) {

    if (requesterId.equals(receiverId)) {
      throw new RuntimeException("You cannot send a friend request to yourself.");
    }

    //1. Check if these people are connected beforehand
    boolean exists = friendshipRepository.existsFriendshipBetween(requesterId, receiverId);
    if (exists) {
      throw new RuntimeException("Friendship or pending request already exists.");
    }

    //2. Check if blocked
    // 2. Check Blocking (Both directions)
    // Check if I blocked them OR if they blocked me
    if (friendshipRepository.isBlocked(requesterId, receiverId) ||
        friendshipRepository.isBlocked(receiverId, requesterId)) {
      throw new RuntimeException("Cannot send request: You are blocked or have blocked this user.");
    }


    // 3. Fetch Entities
    User requester = userRepository.findById(requesterId)
        .orElseThrow(() -> new RuntimeException("Requester not found"));
    User receiver = userRepository.findById(receiverId)
        .orElseThrow(() -> new RuntimeException("Receiver not found"));

    // 4. Create & Save
    Friendship friendship = new Friendship();
    friendship.setRequester(requester);
    friendship.setReceiver(receiver);
    friendship.setStatus(FriendshipStatus.PENDING);

    Friendship savedFriendship = friendshipRepository.save(friendship);

    // 5. Return clean DTO
    return friendshipMapper.toResponse(savedFriendship);
  }

  // person should be able to REJECT requests

  /**
   * Rejects (Declines) a pending request.
   * Usually, we just delete the row so they can request again later (or keep it as DECLINED).
   * For now, let's delete it to keep it simple.
   */
  public void rejectRequest(UUID requestId, UUID currentUserId) {
    Friendship request = friendshipRepository.findById(requestId)
        .orElseThrow(() -> new RuntimeException("Friend request not found"));

    if (!request.getReceiver().getId().equals(currentUserId)) {
      throw new RuntimeException("Unauthorized: You did not receive this request.");
    }

    friendshipRepository.delete(request);
  }

  // person should be able to ACCEPT
  /**
   * Accepts a pending friend request.
   * @param requestId The UUID of the friendship row (NOT the user ID)
   * @param currentUserId The ID of the person clicking "Accept" (Must be the Receiver)
   */
  public FriendshipResponse acceptRequest(UUID requestId, UUID currentUserId) {
    Friendship request = friendshipRepository.findById(requestId)
        .orElseThrow(() -> new RuntimeException("Friend request not found"));

    // SECURITY CHECK: Only the person who received the request can accept it
    if (!request.getReceiver().getId().equals(currentUserId)) {
      throw new RuntimeException("Unauthorized: You did not receive this request.");
    }

    if (request.getStatus() != FriendshipStatus.PENDING) {
      throw new RuntimeException("Request is not pending (Already accepted or handled).");
    }

    request.setStatus(FriendshipStatus.ACCEPTED);
    Friendship saved = friendshipRepository.save(request);

    return friendshipMapper.toResponse(saved);
  }



  /**
   * Returns the status of the relationship (e.g., PENDING, ACCEPTED, NONE).
   * The Frontend uses this to decide which button to show:
   * - "NONE" -> Show "Add Friend" button
   * - "PENDING" -> Show "Request Sent" (grayed out) or "Accept/Decline"
   * - "ACCEPTED" -> Show "Friends" badge
   */
  public String getFriendshipStatus(UUID myId, UUID otherId) {
    if (myId.equals(otherId)) {
      return "SELF"; // Special case: You are looking at your own profile
    }

    // This uses the efficient query you already wrote in the Repository
    return friendshipRepository.findRelationshipStatus(myId, otherId)
        .map(FriendshipStatus::name) // Converts Enum (ACCEPTED) to String "ACCEPTED"
        .orElse("NONE");             // If no row exists, they are strangers
  }



  //list all friend requests when the user wants to see who they friend requested
  /**
   * List who I have sent requests to (so I can remember or cancel them).
   */
  public List<FriendshipResponse> getSentRequests(UUID userId) {

    List<Friendship> requests = friendshipRepository.findSentRequests(userId);

    return requests.stream()
        .map(friendshipMapper::toResponse)
        .collect(Collectors.toList());
  }

  // list all friends of the user
  /**
   * List all my accepted friends.
   */
  public List<UserSummaryDto> getMyFriends(UUID userId) {
    List<User> friends = friendshipRepository.findAllFriends(userId);
    return friends.stream()
        .map(this::convertToUserSummary) // Helper method below
        .collect(Collectors.toList());
  }

  /**
   * List Suggested Friends (Friends of Friends).
   */
  public List<UserSummaryDto> getSuggestedFriends(UUID userId, Pageable pageable) {
    List<User> suggestions = friendshipRepository.findFriendSuggestions(userId, pageable);
    return suggestions.stream()
        .map(this::convertToUserSummary)
        .collect(Collectors.toList());
  }

  /**
   * Search my friend list by name.
   */
  public List<UserSummaryDto> searchFriends(UUID userId, String query) {
    List<User> matches = friendshipRepository.searchFriends(userId, query);
    return matches.stream()
        .map(this::convertToUserSummary)
        .collect(Collectors.toList());
  }

  /**
   * Get Mutual Friends count and list.
   * This is usually two separate calls: one for the number "5 Mutual Friends",
   * and one for the actual list when they click it.
   */
  public long getMutualFriendsCount(UUID myId, UUID otherId) {
    return friendshipRepository.countMutualFriends(myId, otherId);
  }

  public List<UserSummaryDto> getMutualFriends(UUID myId, UUID otherId) {
    List<User> mutuals = friendshipRepository.findMutualFriends(myId, otherId);
    return mutuals.stream()
        .map(this::convertToUserSummary)
        .collect(Collectors.toList());
  }

  // ==========================================
  // 5. COUNTS (For Badges)
  // ==========================================

  /**
   * Get the number of pending requests (Red notification badge).
   */
  public long getPendingRequestCount(UUID userId) {
    return friendshipRepository.countPendingRequests(userId);
  }

  // ==========================================
  // 6. HELPER (Conversion)
  // ==========================================

  /**
   * Quick helper to convert User Entity -> UserSummaryDto
   * (Since we reuse this logic 4 times above)
   */
  private UserSummaryDto convertToUserSummary(User user) {
    return new UserSummaryDto(user.getId(), user.getFirstName(), user.getLastName(),
        user.getEmail());
  }

  // ==========================================
  // 7. ACTIONS (Undo / Unfriend)
  // ==========================================

  /**
   * Cancel a request I sent (Undo).
   * Logic: Verify I am the requester and status is PENDING.
   */
  public void cancelSentRequest(UUID requestId, UUID currentUserId) {
    Friendship request = friendshipRepository.findById(requestId)
        .orElseThrow(() -> new RuntimeException("Request not found"));

    if (!request.getRequester().getId().equals(currentUserId)) {
      throw new RuntimeException("Unauthorized: You did not send this request.");
    }

    if (request.getStatus() != FriendshipStatus.PENDING) {
      throw new RuntimeException("Cannot cancel: Request is already " + request.getStatus());
    }

    friendshipRepository.delete(request);
  }

  /**
   * Unfriend someone.
   * Logic: Find the friendship row (regardless of who requested) and delete it.
   */
  public void unfriendUser(UUID friendId, UUID currentUserId) {
    Friendship friendship = friendshipRepository.findFriendshipBetween(currentUserId, friendId)
        .orElseThrow(() -> new RuntimeException("Friendship not found"));

    friendshipRepository.delete(friendship);
  }


}
