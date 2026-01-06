package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.FriendshipRequest;
import com.parth.sportsapp.sportsbackend.dto.FriendshipResponse;
import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.service.FriendshipService;
import com.parth.sportsapp.sportsbackend.service.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

  @Autowired
  private FriendshipService friendshipService;

  @Autowired
  private JwtUtil jwtUtil;

  /**
   * Send a friend request to the user
   */
  @PostMapping("/request")
  public ResponseEntity<FriendshipResponse> sendRequest(
      @RequestBody FriendshipRequest requestDto, // <--- Catches JSON Body (ID + Message)
      @RequestHeader("Authorization") String token // <--- Catches Header (Sender ID)
  ) {

    // 1. Get who is sending it (from Token)
    UUID senderId = getUserIdFromToken(token);

    // 2. Pass the Sender ID AND the Packet (Receiver ID + Message) to the service
    return ResponseEntity.ok(friendshipService.friendRequest(senderId, requestDto));
  }

  /**
   * Accept a Friend Request
   * URL: POST /api/friends/accept/{requestId}
   */
  @PostMapping("/accept/{requestId}")
  public ResponseEntity<FriendshipResponse> acceptRequest(
      @PathVariable UUID requestId,
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.acceptRequest(requestId, currentUserId));
  }


  @PostMapping("/reject/{requestId}")
  public ResponseEntity<FriendshipResponse> rejectRequest(@PathVariable UUID requestId,
      @RequestHeader("Authorization") String token) {
    UUID currentUserId = getUserIdFromToken(token);
    friendshipService.rejectRequest(requestId, currentUserId);

    return ResponseEntity.noContent().build();
  }

  /**
   * Cancel a Request I sent (Undo)
   * URL: DELETE /api/friends/cancel/{requestId}
   */
  @DeleteMapping("/cancel/{requestId}")
  public ResponseEntity<Void> cancelRequest(
      @PathVariable UUID requestId,
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    friendshipService.cancelSentRequest(requestId, currentUserId);
    return ResponseEntity.noContent().build();
  }

  //requestId is the UUID of the relationship
  @DeleteMapping("/delete/{friendID}")
  public ResponseEntity<Void> deleteRequest(@PathVariable UUID friendID,
      @RequestHeader("Authorization") String token) {
    UUID currentUserId = getUserIdFromToken(token);
    friendshipService.unfriendUser(friendID, currentUserId);
    return ResponseEntity.noContent().build();
  }


  /**
   * Check status with another user (For Profile Buttons)
   * URL: GET /api/friends/status/{otherUserId}
   * Returns: "NONE", "PENDING", "ACCEPTED", "SELF"
   */
  @GetMapping("/status/{otherUserId}")
  public ResponseEntity<String> getStatus(
      @PathVariable UUID otherUserId,
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.getFriendshipStatus(currentUserId, otherUserId));
  }

  /**
   * Get My Friend List
   * URL: GET /api/friends
   */
  @GetMapping
  public ResponseEntity<List<UserSummaryDto>> getMyFriends(
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.getMyFriends(currentUserId));
  }

  /**
   * Get Received Requests (My Inbox)
   * URL: GET /api/friends/requests/received
   */
  @GetMapping("/requests/received")
  public ResponseEntity<List<FriendshipResponse>> getReceivedRequests(
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.getReceivedRequests(currentUserId));
  }

  /**
   * Get Sent Requests (Who I am waiting for)
   * URL: GET /api/friends/requests/sent
   */
  @GetMapping("/requests/sent")
  public ResponseEntity<List<FriendshipResponse>> getSentRequests(
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.getSentRequests(currentUserId));
  }

  /**
   * Get Suggested Friends
   * URL: GET /api/friends/suggestions
   */
  @GetMapping("/suggestions")
  public ResponseEntity<List<UserSummaryDto>> getSuggestions(
      @RequestHeader("Authorization") String token,
      Pageable pageable) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.getSuggestedFriends(currentUserId, pageable));
  }

  /**
   * Search within my friends
   * URL: GET /api/friends/search?query=John
   */
  @GetMapping("/search")
  public ResponseEntity<List<UserSummaryDto>> searchMyFriends(
      @RequestParam String query,
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.searchFriends(currentUserId, query));
  }

  /**
   * Get Mutual Friends
   * URL: GET /api/friends/mutual/{otherUserId}
   */
  @GetMapping("/mutual/{otherUserId}")
  public ResponseEntity<List<UserSummaryDto>> getMutualFriends(
      @PathVariable UUID otherUserId,
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.getMutualFriends(currentUserId, otherUserId));
  }

  // =====================================================================
  // 4. STATS (Badges & Counts)
  // =====================================================================

  /**
   * Get Notification Count (Red Badge)
   * URL: GET /api/friends/requests/count
   */
  @GetMapping("/requests/count")
  public ResponseEntity<Long> getPendingRequestCount(
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.getPendingRequestCount(currentUserId));
  }

  /**
   * Get Total Friend Count
   * URL: GET /api/friends/count
   */
  @GetMapping("/count")
  public ResponseEntity<Long> getFriendCount(
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.getFriendCount(currentUserId));
  }

  /**
   * Batch Accept Friend Requests
   * URL: POST /api/friends/accept-batch
   * Body: [ "uuid-1", "uuid-2", "uuid-3" ]
   */
  @PostMapping("/accept-batch")
  public ResponseEntity<List<FriendshipResponse>> acceptMultipleRequests(
      @RequestBody List<UUID> requestIds,
      @RequestHeader("Authorization") String token) {

    UUID currentUserId = getUserIdFromToken(token);
    return ResponseEntity.ok(friendshipService.acceptMultipleRequests(requestIds, currentUserId));
  }



  private UUID getUserIdFromToken(String token) {
    if (token != null && token.startsWith("Bearer ")) {
      String jwt = token.substring(7);
      return UUID.fromString(jwtUtil.extractUserId(jwt));
    }
    throw new RuntimeException("Invalid Token");
  }
}
