package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Friendship;
import com.parth.sportsapp.sportsbackend.model.FriendshipStatus;
import com.parth.sportsapp.sportsbackend.model.User;
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

  //count total number of FRIENDS
@Query("SELECT COUNT(f) FROM Friendship f where " +
    "(f.requester.id = :userId OR f.receiver.id = :userId)" +
    "AND f.status = 'ACCEPTED'")
  long countAcceptedFriends(@Param("userId") UUID userId);

  @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
      "((f.requester.id = :myId AND f.receiver.id = :otherId) OR " +
      "(f.requester.id = :otherId AND f.receiver.id = :myId)) " +
      "AND f.status = 'ACCEPTED'")
  boolean areFriends(@Param("myId") UUID myId, @Param("otherId") UUID otherId);



  @Query("SELECT f FROM Friendship f WHERE f.receiver.id = :userId AND f.status = 'PENDING'")
  List<Friendship> findPendingRequests(@Param("userId") UUID userId);


// finds the friendships in between 2 people when the user clicks on that profile
@Query("SELECT f FROM Friendship f WHERE " +
    "((f.requester.id = :user1 AND f.receiver.id = :user2) OR " +
    "(f.requester.id = :user2 AND f.receiver.id = :user1))")
  Optional<Friendship> findFriendshipBetween(@Param("user1") UUID user1, @Param("user2") UUID user2);

  // Find sent requests (to see who I've requested)
  @Query("SELECT f FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'PENDING'")
  List<Friendship> findSentRequests(@Param("userId") UUID userId);

  @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
      "((f.requester.id = :user1 AND f.receiver.id = :user2) OR " +
      "(f.requester.id = :user2 AND f.receiver.id = :user1))")
  boolean existsFriendshipBetween(@Param("user1") UUID user1, @Param("user2") UUID user2);

  @Query("SELECT CASE WHEN f.requester.id = :userId THEN f.receiver ELSE f.requester END " +
      "FROM Friendship f WHERE " +
      "(f.requester.id = :userId OR f.receiver.id = :userId) " +
      "AND f.status = 'ACCEPTED'")
  List<User> findAllFriends(@Param("userId") UUID userId);

  // 5. Get all accepted friends with pagination
  @Query("SELECT CASE WHEN f.requester.id = :userId THEN f.receiver ELSE f.requester END " +
      "FROM Friendship f WHERE " +
      "(f.requester.id = :userId OR f.receiver.id = :userId) " +
      "AND f.status = 'ACCEPTED' " +
      "ORDER BY f.createdAt DESC")
  Page<User> findAllFriendsPaginated(@Param("userId") UUID userId, Pageable pageable);

  // 6. Find mutual friends between two users
  @Query("SELECT CASE WHEN f1.requester.id = :user1 THEN f1.receiver ELSE f1.requester END " +
      "FROM Friendship f1, Friendship f2 WHERE " +
      "((f1.requester.id = :user1 OR f1.receiver.id = :user1) AND f1.status = 'ACCEPTED') " +
      "AND ((f2.requester.id = :user2 OR f2.receiver.id = :user2) AND f2.status = 'ACCEPTED') " +
      "AND (CASE WHEN f1.requester.id = :user1 THEN f1.receiver.id ELSE f1.requester.id END) = " +
      "(CASE WHEN f2.requester.id = :user2 THEN f2.receiver.id ELSE f2.requester.id END)")
  List<User> findMutualFriends(@Param("user1") UUID user1, @Param("user2") UUID user2);

  // 7. Count mutual friends (for display like "5 mutual friends")
  // 7. Count mutual friends (Optimized)
  @Query("SELECT COUNT(f1) FROM Friendship f1 WHERE " +
      "(f1.requester.id = :user1 OR f1.receiver.id = :user1) " +
      "AND f1.status = 'ACCEPTED' " +
      "AND (CASE WHEN f1.requester.id = :user1 THEN f1.receiver.id ELSE f1.requester.id END) IN " +
      "(SELECT CASE WHEN f2.requester.id = :user2 THEN f2.receiver.id ELSE f2.requester.id END " +
      " FROM Friendship f2 WHERE " +
      " (f2.requester.id = :user2 OR f2.receiver.id = :user2) " +
      " AND f2.status = 'ACCEPTED')")
  long countMutualFriends(@Param("user1") UUID user1, @Param("user2") UUID user2);

  // 8. Friend suggestions (friends of friends who aren't your friends yet)
  @Query("SELECT DISTINCT CASE WHEN f2.requester.id = " +
      "(CASE WHEN f1.requester.id = :userId THEN f1.receiver.id ELSE f1.requester.id END) " +
      "THEN f2.receiver ELSE f2.requester END " +
      "FROM Friendship f1, Friendship f2 WHERE " +
      "((f1.requester.id = :userId OR f1.receiver.id = :userId) AND f1.status = 'ACCEPTED') " +
      "AND ((f2.requester.id = (CASE WHEN f1.requester.id = :userId THEN f1.receiver.id ELSE f1.requester.id END) " +
      "OR f2.receiver.id = (CASE WHEN f1.requester.id = :userId THEN f1.receiver.id ELSE f1.requester.id END)) " +
      "AND f2.status = 'ACCEPTED') " +
      "AND (CASE WHEN f2.requester.id = (CASE WHEN f1.requester.id = :userId THEN f1.receiver.id ELSE f1.requester.id END) " +
      "THEN f2.receiver.id ELSE f2.requester.id END) != :userId " +
      "AND NOT EXISTS (SELECT f3 FROM Friendship f3 WHERE " +
      "((f3.requester.id = :userId AND f3.receiver.id = " +
      "(CASE WHEN f2.requester.id = (CASE WHEN f1.requester.id = :userId THEN f1.receiver.id ELSE f1.requester.id END) " +
      "THEN f2.receiver.id ELSE f2.requester.id END)) OR " +
      "(f3.receiver.id = :userId AND f3.requester.id = " +
      "(CASE WHEN f2.requester.id = (CASE WHEN f1.requester.id = :userId THEN f1.receiver.id ELSE f1.requester.id END) " +
      "THEN f2.receiver.id ELSE f2.requester.id END))) AND f3.status = 'ACCEPTED')")
  List<User> findFriendSuggestions(@Param("userId") UUID userId, Pageable pageable);

  // 9. Search friends by name (for search functionality in friend list)
  @Query("SELECT CASE WHEN f.requester.id = :userId THEN f.receiver ELSE f.requester END " +
      "FROM Friendship f WHERE " +
      "(f.requester.id = :userId OR f.receiver.id = :userId) " +
      "AND f.status = 'ACCEPTED' " +
      "AND (" +
      "LOWER(CASE WHEN f.requester.id = :userId THEN f.receiver.firstName ELSE f.requester.firstName END) " +
      "LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
      "OR LOWER(CASE WHEN f.requester.id = :userId THEN f.receiver.lastName ELSE f.requester.lastName END) " +
      "LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
  List<User> searchFriends(@Param("userId") UUID userId, @Param("searchTerm") String searchTerm);

  // 10. Count pending requests received (for notification badge)
  @Query("SELECT COUNT(f) FROM Friendship f WHERE f.receiver.id = :userId AND f.status = 'PENDING'")
  long countPendingRequests(@Param("userId") UUID userId);

  // 11. Count sent requests (to show in "Sent Requests" section)
  @Query("SELECT COUNT(f) FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'PENDING'")
  long countSentRequests(@Param("userId") UUID userId);

  // 12. Recently added friends (for "Recent Friends" section)
  @Query("SELECT CASE WHEN f.requester.id = :userId THEN f.receiver ELSE f.requester END " +
      "FROM Friendship f WHERE " +
      "(f.requester.id = :userId OR f.receiver.id = :userId) " +
      "AND f.status = 'ACCEPTED' " +
      "ORDER BY f.createdAt DESC")
  List<User> findRecentFriends(@Param("userId") UUID userId, Pageable pageable);

  // 13. Find all blocked relationships (if implementing blocking)
  @Query("SELECT f FROM Friendship f WHERE " +
      "f.requester.id = :userId AND f.status = 'BLOCKED'")
  List<Friendship> findBlockedUsers(@Param("userId") UUID userId);

  // 14. Check if blocked
  @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
      "f.requester.id = :blockerId AND f.receiver.id = :blockedId AND f.status = 'BLOCKED'")
  boolean isBlocked(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId);

  // 15. Check relationship status (returns the status enum or null)
  @Query("SELECT f.status FROM Friendship f WHERE " +
      "((f.requester.id = :user1 AND f.receiver.id = :user2) OR " +
      "(f.requester.id = :user2 AND f.receiver.id = :user1))")
  Optional<FriendshipStatus> findRelationshipStatus(@Param("user1") UUID user1, @Param("user2") UUID user2);



}
