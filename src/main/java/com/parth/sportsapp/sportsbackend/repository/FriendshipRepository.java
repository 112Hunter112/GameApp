package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Friendship;
import com.parth.sportsapp.sportsbackend.model.FriendshipStatus; // Ensure this exists or use String
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

  // 1. Check if friends (Status check)
  @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
      "((f.requester.id = :myId AND f.receiver.id = :otherId) OR " +
      "(f.requester.id = :otherId AND f.receiver.id = :myId)) " +
      "AND f.status = 'ACCEPTED'")
  boolean areFriends(@Param("myId") UUID myId, @Param("otherId") UUID otherId);

  // 2. Find specific relationship
  @Query("SELECT f FROM Friendship f WHERE " +
      "((f.requester.id = :user1 AND f.receiver.id = :user2) OR " +
      "(f.requester.id = :user2 AND f.receiver.id = :user1))")
  Optional<Friendship> findFriendshipBetween(@Param("user1") UUID user1, @Param("user2") UUID user2);

  // 3. Pending Requests (Received)
  @Query("SELECT f FROM Friendship f WHERE f.receiver.id = :userId AND f.status = 'PENDING'")
  List<Friendship> findPendingRequests(@Param("userId") UUID userId);

  // 4. Sent Requests
  @Query("SELECT f FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'PENDING'")
  List<Friendship> findSentRequests(@Param("userId") UUID userId);

  // ==========================================
  // ⚠️ FIXED METHODS (Return Friendship, not User)
  // ==========================================

  // 5. Get all accepted friends
  // Removed CASE. Just get the relationship. Logic moves to Java.
  @Query("SELECT f FROM Friendship f WHERE " +
      "(f.requester.id = :userId OR f.receiver.id = :userId) " +
      "AND f.status = 'ACCEPTED'")
  List<Friendship> findAllFriends(@Param("userId") UUID userId);

  // 6. Get all accepted friends with pagination
  @Query("SELECT f FROM Friendship f WHERE " +
      "(f.requester.id = :userId OR f.receiver.id = :userId) " +
      "AND f.status = 'ACCEPTED' " +
      "ORDER BY f.createdAt DESC")
  Page<Friendship> findAllFriendsPaginated(@Param("userId") UUID userId, Pageable pageable);

  // 7. Search friends
  // We filter by checking BOTH users in the relationship against the search term
  @Query("SELECT f FROM Friendship f WHERE " +
      "(f.requester.id = :userId OR f.receiver.id = :userId) " +
      "AND f.status = 'ACCEPTED' " +
      "AND (" +
      "   (f.requester.id != :userId AND (LOWER(f.requester.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(f.requester.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))) " +
      "   OR " +
      "   (f.receiver.id != :userId AND (LOWER(f.receiver.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(f.receiver.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))) " +
      ")")
  List<Friendship> searchFriends(@Param("userId") UUID userId, @Param("searchTerm") String searchTerm);

  // 8. Recent Friends
  @Query("SELECT f FROM Friendship f WHERE " +
      "(f.requester.id = :userId OR f.receiver.id = :userId) " +
      "AND f.status = 'ACCEPTED' " +
      "ORDER BY f.createdAt DESC")
  List<Friendship> findRecentFriends(@Param("userId") UUID userId, Pageable pageable);

  // ==========================================
  // COUNTS & CHECKS
  // ==========================================

  @Query("SELECT COUNT(f) FROM Friendship f WHERE f.receiver.id = :userId AND f.status = 'PENDING'")
  long countPendingRequests(@Param("userId") UUID userId);

  @Query("SELECT COUNT(f) FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'PENDING'")
  long countSentRequests(@Param("userId") UUID userId);

  @Query("SELECT COUNT(f) FROM Friendship f WHERE " +
      "(f.requester.id = :userId OR f.receiver.id = :userId) " +
      "AND f.status = 'ACCEPTED'")
  long countAcceptedFriends(@Param("userId") UUID userId);

  @Query("SELECT f.status FROM Friendship f WHERE " +
      "((f.requester.id = :user1 AND f.receiver.id = :user2) OR " +
      "(f.requester.id = :user2 AND f.receiver.id = :user1))")
  Optional<String> findRelationshipStatus(@Param("user1") UUID user1, @Param("user2") UUID user2);
}
