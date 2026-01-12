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

  @Autowired private FriendshipService friendshipService;
  @Autowired private JwtUtil jwtUtil;

  // Helper: Extract User ID
  private UUID getUserIdFromToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new RuntimeException("Invalid Token");
    }
    String token = authHeader.substring(7);
    return UUID.fromString(jwtUtil.extractUserId(token));
  }

  // --- REQUESTS ---

  @PostMapping("/request")
  public ResponseEntity<FriendshipResponse> sendRequest(
      @RequestBody FriendshipRequest requestDto,
      @RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.friendRequest(getUserIdFromToken(token), requestDto));
  }

  @PostMapping("/accept/{requestId}")
  public ResponseEntity<FriendshipResponse> acceptRequest(
      @PathVariable UUID requestId,
      @RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.acceptRequest(requestId, getUserIdFromToken(token)));
  }

  @PostMapping("/reject/{requestId}")
  public ResponseEntity<Void> rejectRequest(
      @PathVariable UUID requestId,
      @RequestHeader("Authorization") String token) {
    friendshipService.rejectRequest(requestId, getUserIdFromToken(token));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/cancel/{requestId}")
  public ResponseEntity<Void> cancelRequest(
      @PathVariable UUID requestId,
      @RequestHeader("Authorization") String token) {
    friendshipService.cancelSentRequest(requestId, getUserIdFromToken(token));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/delete/{friendId}")
  public ResponseEntity<Void> unfriend(
      @PathVariable UUID friendId,
      @RequestHeader("Authorization") String token) {
    friendshipService.unfriendUser(friendId, getUserIdFromToken(token));
    return ResponseEntity.noContent().build();
  }

  // --- INFO & LISTS ---

  @GetMapping("/status/{otherUserId}")
  public ResponseEntity<String> getStatus(
      @PathVariable UUID otherUserId,
      @RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.getFriendshipStatus(getUserIdFromToken(token), otherUserId));
  }

  @GetMapping
  public ResponseEntity<List<UserSummaryDto>> getMyFriends(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.getMyFriends(getUserIdFromToken(token)));
  }

  @GetMapping("/requests/received")
  public ResponseEntity<List<FriendshipResponse>> getReceivedRequests(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.getReceivedRequests(getUserIdFromToken(token)));
  }

  @GetMapping("/requests/sent")
  public ResponseEntity<List<FriendshipResponse>> getSentRequests(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.getSentRequests(getUserIdFromToken(token)));
  }

  @GetMapping("/suggestions")
  public ResponseEntity<List<UserSummaryDto>> getSuggestions(
      @RequestHeader("Authorization") String token, Pageable pageable) {
    return ResponseEntity.ok(friendshipService.getSuggestedFriends(getUserIdFromToken(token), pageable));
  }

  @GetMapping("/search")
  public ResponseEntity<List<UserSummaryDto>> searchFriends(
      @RequestParam String query,
      @RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.searchFriends(getUserIdFromToken(token), query));
  }

  @GetMapping("/mutual/{otherUserId}")
  public ResponseEntity<List<UserSummaryDto>> getMutualFriends(
      @PathVariable UUID otherUserId,
      @RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.getMutualFriends(getUserIdFromToken(token), otherUserId));
  }

  // --- BATCH & COUNTS ---

  @GetMapping("/requests/count")
  public ResponseEntity<Long> getPendingCount(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.getPendingRequestCount(getUserIdFromToken(token)));
  }

  @GetMapping("/count")
  public ResponseEntity<Long> getFriendCount(@RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.getFriendCount(getUserIdFromToken(token)));
  }

  @PostMapping("/accept-batch")
  public ResponseEntity<List<FriendshipResponse>> acceptBatch(
      @RequestBody List<UUID> requestIds,
      @RequestHeader("Authorization") String token) {
    return ResponseEntity.ok(friendshipService.acceptMultipleRequests(requestIds, getUserIdFromToken(token)));
  }
}
