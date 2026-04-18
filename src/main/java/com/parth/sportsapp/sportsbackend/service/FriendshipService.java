package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.FriendshipRequest;
import com.parth.sportsapp.sportsbackend.dto.FriendshipResponse;
import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.mapper.FriendshipMapper;
import com.parth.sportsapp.sportsbackend.model.Friendship;
import com.parth.sportsapp.sportsbackend.model.FriendshipStatus;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.FriendshipRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

  @Autowired private UserRepository userRepository;
  @Autowired private FriendshipRepository friendshipRepository;
  @Autowired private FriendshipMapper friendshipMapper;

  // =====================================================================
  // 🛡️ HELPER: The Logic Hibernate Couldn't Handle
  // =====================================================================
  /**
   * EXTRACTS the "Other Person" from a Friendship object.
   * If I am the requester, the friend is the receiver.
   * If I am the receiver, the friend is the requester.
   */
  private User getFriendFromFriendship(Friendship f, UUID myId) {
    if (f.getRequester().getId().equals(myId)) {
      return f.getReceiver();
    } else {
      return f.getRequester();
    }
  }

  private UserSummaryDto convertToUserSummary(User user) {
    return new UserSummaryDto(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        user.getUsername(),
        user.getEmail(),
        user.getBio(),
        user.getProfilePictureUrl()
    );
  }

  // =====================================================================
  // 1. FRIEND REQUESTS (Send / Accept / Reject)
  // =====================================================================

  @Transactional
  public FriendshipResponse friendRequest(UUID requesterId, FriendshipRequest requestDto) {
    UUID receiverId = requestDto.getReceiverId();

    if (requesterId.equals(receiverId)) {
      throw new RuntimeException("You cannot send a friend request to yourself.");
    }

    // Check if connection exists (using Boolean query which is safe)
    if (friendshipRepository.areFriends(requesterId, receiverId) ||
        friendshipRepository.findFriendshipBetween(requesterId, receiverId).isPresent()) {
      throw new RuntimeException("Friendship or pending request already exists.");
    }

    User requester = userRepository.findById(requesterId)
        .orElseThrow(() -> new RuntimeException("Requester not found"));
    User receiver = userRepository.findById(receiverId)
        .orElseThrow(() -> new RuntimeException("Receiver not found"));

    Friendship friendship = new Friendship();
    friendship.setRequester(requester);
    friendship.setReceiver(receiver);
    friendship.setStatus(FriendshipStatus.PENDING);
    friendship.setMessage(requestDto.getMessage());

    return friendshipMapper.toResponse(friendshipRepository.save(friendship));
  }

  @Transactional
  public FriendshipResponse acceptRequest(UUID requestId, UUID currentUserId) {
    Friendship request = friendshipRepository.findById(requestId)
        .orElseThrow(() -> new RuntimeException("Friend request not found"));

    if (!request.getReceiver().getId().equals(currentUserId)) {
      throw new RuntimeException("Unauthorized: You did not receive this request.");
    }

    if (request.getStatus() != FriendshipStatus.PENDING) {
      throw new RuntimeException("Request is not pending.");
    }

    request.setStatus(FriendshipStatus.ACCEPTED);
    return friendshipMapper.toResponse(friendshipRepository.save(request));
  }

  @Transactional
  public void rejectRequest(UUID requestId, UUID currentUserId) {
    Friendship request = friendshipRepository.findById(requestId)
        .orElseThrow(() -> new RuntimeException("Friend request not found"));

    if (!request.getReceiver().getId().equals(currentUserId)) {
      throw new RuntimeException("Unauthorized");
    }
    friendshipRepository.delete(request);
  }

  @Transactional
  public void cancelSentRequest(UUID requestId, UUID currentUserId) {
    Friendship request = friendshipRepository.findById(requestId)
        .orElseThrow(() -> new RuntimeException("Request not found"));

    if (!request.getRequester().getId().equals(currentUserId)) {
      throw new RuntimeException("Unauthorized");
    }
    friendshipRepository.delete(request);
  }

  @Transactional
  public void unfriendUser(UUID friendId, UUID currentUserId) {
    Friendship friendship = friendshipRepository.findFriendshipBetween(currentUserId, friendId)
        .orElseThrow(() -> new RuntimeException("Friendship not found"));
    friendshipRepository.delete(friendship);
  }

  // =====================================================================
  // 2. LISTS (The Crash Fix)
  // =====================================================================

  /**
   * Fixed: Gets Friendship objects and extracts Users in Java.
   */
  @Transactional(readOnly = true)
  public List<UserSummaryDto> getMyFriends(UUID userId) {
    return friendshipRepository.findAllFriends(userId).stream()
        .map(f -> getFriendFromFriendship(f, userId)) // Java Logic
        .map(this::convertToUserSummary)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<UserSummaryDto> searchFriends(UUID userId, String query) {
    return friendshipRepository.searchFriends(userId, query).stream()
        .map(f -> getFriendFromFriendship(f, userId)) // Java Logic
        .map(this::convertToUserSummary)
        .collect(Collectors.toList());
  }

  /**
   * Logic: Get MY friends, then get THEIR friends, remove duplicates & me.
   * Safer than complex SQL for now.
   */
  @Transactional(readOnly = true)
  public List<UserSummaryDto> getSuggestedFriends(UUID userId, Pageable pageable) {
    // 1. Get my friends
    List<User> myFriends = friendshipRepository.findAllFriends(userId).stream()
        .map(f -> getFriendFromFriendship(f, userId))
        .collect(Collectors.toList());

    Set<UUID> myFriendIds = myFriends.stream().map(User::getId).collect(Collectors.toSet());
    myFriendIds.add(userId); // Exclude myself

    // 2. Find friends of friends
    Set<User> suggestions = new HashSet<>();

    for (User friend : myFriends) {
      // Get friends of this friend
      List<Friendship> friendsOfFriend = friendshipRepository.findAllFriends(friend.getId());
      for (Friendship f : friendsOfFriend) {
        User candidate = getFriendFromFriendship(f, friend.getId());
        // If I'm not already friends with them and it's not me
        if (!myFriendIds.contains(candidate.getId())) {
          suggestions.add(candidate);
        }
      }
      if (suggestions.size() >= pageable.getPageSize()) break; // Limit logic
    }

    return suggestions.stream()
        .limit(pageable.getPageSize())
        .map(this::convertToUserSummary)
        .collect(Collectors.toList());
  }

  /**
   * Logic: Get intersection of Friend List A and Friend List B.
   */
  @Transactional(readOnly = true)
  public List<UserSummaryDto> getMutualFriends(UUID user1, UUID user2) {
    // Get friends of User 1
    Set<UUID> friendsOf1 = friendshipRepository.findAllFriends(user1).stream()
        .map(f -> getFriendFromFriendship(f, user1).getId())
        .collect(Collectors.toSet());

    // Get friends of User 2 and check intersection
    return friendshipRepository.findAllFriends(user2).stream()
        .map(f -> getFriendFromFriendship(f, user2))
        .filter(u -> friendsOf1.contains(u.getId())) // Intersection check
        .map(this::convertToUserSummary)
        .collect(Collectors.toList());
  }

  // =====================================================================
  // 3. STATUS & INFO
  // =====================================================================

  public String getFriendshipStatus(UUID myId, UUID otherId) {
    if (myId.equals(otherId)) return "SELF";

    return friendshipRepository.findRelationshipStatus(myId, otherId)
        .orElse("NONE"); // Returns String "ACCEPTED", "PENDING", or "NONE"
  }

  public List<FriendshipResponse> getReceivedRequests(UUID userId) {
    return friendshipRepository.findPendingRequests(userId).stream()
        .map(friendshipMapper::toResponse)
        .collect(Collectors.toList());
  }

  public List<FriendshipResponse> getSentRequests(UUID userId) {
    return friendshipRepository.findSentRequests(userId).stream()
        .map(friendshipMapper::toResponse)
        .collect(Collectors.toList());
  }

  public long getPendingRequestCount(UUID userId) {
    return friendshipRepository.countPendingRequests(userId);
  }

  public long getFriendCount(UUID userId) {
    return friendshipRepository.countAcceptedFriends(userId);
  }

  // =====================================================================
  // 4. BATCH
  // =====================================================================

  @Transactional
  public List<FriendshipResponse> acceptMultipleRequests(List<UUID> requestIds, UUID currentUserId) {
    List<Friendship> requests = friendshipRepository.findAllById(requestIds);
    List<Friendship> toSave = new ArrayList<>();

    for (Friendship request : requests) {
      if (request.getReceiver().getId().equals(currentUserId) &&
          request.getStatus() == FriendshipStatus.PENDING) {
        request.setStatus(FriendshipStatus.ACCEPTED);
        toSave.add(request);
      }
    }
    return friendshipRepository.saveAll(toSave).stream()
        .map(friendshipMapper::toResponse)
        .collect(Collectors.toList());
  }
}
