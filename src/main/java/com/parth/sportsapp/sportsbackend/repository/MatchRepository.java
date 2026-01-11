package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Match;
import com.parth.sportsapp.sportsbackend.model.MatchSource;
import com.parth.sportsapp.sportsbackend.model.MatchVerificationStatus;
import com.parth.sportsapp.sportsbackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

  // Match results (Searching by score is rare, but here is how you'd do it)
  // Example: Find matches where the score was exactly "6-4, 6-4"
  List<Match> findByScore(String score);


  // Source of the Booking
  // "Find all matches that were manually entered" -> findBySource(MatchSource.MANUAL_ENTRY)
  List<Match> findBySource(MatchSource source);

  //Verification status
  // "Find all the status of each event
  List<Match> findByVerificationStatus(MatchVerificationStatus status);

  //Find all Matches that are PENDING and have the email of the new joining User


  // "Find manual entries created by me that are still pending"
  List<Match> findByCreatedByUserAndSourceAndVerificationStatus(
      User user,
      MatchSource source,
      MatchVerificationStatus status
  );


  //matches made by user
  List<Match> findByCreatedByUser(User user);


  //"Pending Invites" - Matches where I am invited but haven't accepted
  // (You'll need to pass "PENDING" as the status)
  List<Match> findByParticipants_Id_UserIdAndParticipants_Status(UUID userId, String status);

// list all the recent matches in order
  List<Match> findByParticipants_User_IdOrderByMatchDateDesc(UUID userId);

  @Query("SELECT m FROM Match m JOIN m.participants p " +
      "WHERE p.user.id = :userId " +
      "AND m.verificationStatus = 'PENDING' " +
      "AND m.createdByUser.id != :userId")
  List<Match> findPendingVerifications(@Param("userId") UUID userId);

  // Upcoming Schedule
  // todo change all list to pageable
  List<Match> findByParticipants_User_IdAndMatchDateAfterOrderByMatchDateAsc(UUID userId, LocalDateTime now);

  // Past History
  // todo change all list to pageable
  List<Match> findByParticipants_User_IdAndMatchDateBeforeOrderByMatchDateDesc(UUID userId, LocalDateTime now);

  // My Full History (Paginated & Ordered)
  // Usage: matchRepository.findHistory(userId, PageRequest.of(0, 10));
  @Query("SELECT m FROM Match m JOIN m.participants p WHERE p.user.id = :userId ORDER BY m.matchDate DESC")
  Page<Match> findHistory(@Param("userId") UUID userId, Pageable pageable);


  //Upcoming Games (For the Dashboard)
  @Query("SELECT m FROM Match m JOIN m.participants p WHERE p.user.id = :userId AND m.matchDate > CURRENT_TIMESTAMP ORDER BY m.matchDate ASC")
  List<Match> findUpcomingMatches(@Param("userId") UUID userId);

  // Head-to-Head (Rivalry), shows list of people who you went up against
  @Query("SELECT m FROM Match m JOIN m.participants p1 JOIN m.participants p2 " +
      "WHERE p1.user.id = :user1Id AND p2.user.id = :user2Id " +
      "ORDER BY m.matchDate DESC")
  List<Match> findHeadToHead(@Param("user1Id") UUID user1Id, @Param("user2Id") UUID user2Id);

  // Count total matches a user participated in
  @Query("SELECT COUNT(m) FROM Match m JOIN m.participants p WHERE p.user.id = :userId")
  long countTotalMatches(@Param("userId") UUID userId);

  // Count losses (all matches where user participated but didn't win)
  @Query("SELECT COUNT(m) FROM Match m JOIN m.participants p " +
      "WHERE p.user.id = :userId " +
      "AND m.winningTeam IS NOT NULL " +
      "AND m.winningTeam != 'DRAW' " +
      "AND m.winningTeam != p.teamName")
  long countLosses(@Param("userId") UUID userId);

  // Matches this month
  @Query("SELECT m FROM Match m JOIN m.participants p " +
      "WHERE p.user.id = :userId " +
      "AND m.matchDate >= :startOfMonth " +
      "AND m.matchDate < :endOfMonth " +
      "ORDER BY m.matchDate DESC")
  List<Match> findMatchesThisMonth(@Param("userId") UUID userId,
      @Param("startOfMonth") LocalDateTime startOfMonth,
      @Param("endOfMonth") LocalDateTime endOfMonth);

  // Wins this month
  @Query("SELECT COUNT(m) FROM Match m JOIN m.participants p " +
      "WHERE p.user.id = :userId " +
      "AND m.winningTeam = p.teamName " +
      "AND m.matchDate >= :startOfMonth " +
      "AND m.matchDate < :endOfMonth")
  long countWinsThisMonth(@Param("userId") UUID userId,
      @Param("startOfMonth") LocalDateTime startOfMonth,
      @Param("endOfMonth") LocalDateTime endOfMonth);


  // Find all opponents (users who appear in matches with me)
  @Query("SELECT p2.user, COUNT(m) as matchCount FROM Match m " +
      "JOIN m.participants p1 " +
      "JOIN m.participants p2 " +
      "WHERE p1.user.id = :userId " +
      "AND p2.user.id != :userId " +
      "GROUP BY p2.user " +
      "ORDER BY matchCount DESC")
  List<Object[]> findMostPlayedOpponents(@Param("userId") UUID userId, Pageable pageable);
  // Returns: [User object, match count]

  // Already have this with pagination, but add a convenience method:
  @Query("SELECT m FROM Match m JOIN m.participants p " +
      "WHERE p.user.id = :userId " +
      "ORDER BY m.matchDate DESC")
  List<Match> findRecentMatches(@Param("userId") UUID userId, Pageable pageable);
  // Usage: findRecentMatches(userId, PageRequest.of(0, 10))

  @Query("SELECT m FROM Match m JOIN m.participants p " +
      "WHERE p.user.id = :userId " +
      "AND m.matchDate BETWEEN :startDate AND :endDate " +
      "ORDER BY m.matchDate DESC")
  List<Match> findMatchesByDateRange(@Param("userId") UUID userId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  // Today's matches
  @Query("SELECT m FROM Match m JOIN m.participants p " +
      "WHERE p.user.id = :userId " +
      "AND m.matchDate BETWEEN :startOfDay AND :endOfDay " +
      "ORDER BY m.matchDate ASC")
  List<Match> findMatchesToday(@Param("userId") UUID userId,
      @Param("startOfDay") LocalDateTime startOfDay,
      @Param("endOfDay") LocalDateTime endOfDay);

  // This week's matches
  @Query("SELECT m FROM Match m JOIN m.participants p " +
      "WHERE p.user.id = :userId " +
      "AND m.matchDate >= :startOfWeek " +
      "AND m.matchDate < :endOfWeek " +
      "ORDER BY m.matchDate ASC")
  List<Match> findMatchesThisWeek(@Param("userId") UUID userId,
      @Param("startOfWeek") LocalDateTime startOfWeek,
      @Param("endOfWeek") LocalDateTime endOfWeek);

  // Get last N matches to calculate streak in service layer
  @Query("SELECT m FROM Match m JOIN m.participants p " +
      "WHERE p.user.id = :userId " +
      "AND m.winningTeam IS NOT NULL " +
      "ORDER BY m.matchDate DESC")
  List<Match> findRecentMatchesForStreak(@Param("userId") UUID userId, Pageable pageable);
  // Get last 20, then calculate streak in service

  @Query("SELECT m FROM Match m " +
      "JOIN m.participants p " +
      "JOIN m.booking b " +
      "JOIN b.court c " +
      "WHERE p.user.id = :userId " +
      "AND c.venue.id = :venueId " +
      "ORDER BY m.matchDate DESC")
  List<Match> findMatchesAtVenue(@Param("userId") UUID userId,
      @Param("venueId") UUID venueId);


  @Query("SELECT m FROM Match m " +
      "JOIN m.participants p " +
      "WHERE p.user.id IN :friendIds " +
      "AND m.matchDate >= :since " +
      "ORDER BY m.matchDate DESC")
  List<Match> findFriendsRecentActivity(@Param("friendIds") List<UUID> friendIds,
      @Param("since") LocalDateTime since,
      Pageable pageable);


  // Find matches where the opponent hasn't joined yet
  List<Match> findByExternalOpponentEmail(String email);

  // Count Total Wins (Lifetime)
  // MatchRepository.java
  @Query("SELECT COUNT(m) FROM Match m JOIN m.participants p " +
      "WHERE p.user.id = :userId " +
      "AND m.winningTeam = p.teamName") // Crucial: compare strings
  long countTotalWins(@Param("userId") UUID userId);


  @Query("SELECT m FROM Match m JOIN m.participants p WHERE p.user.id = :userId AND (m.score IS NULL OR m.score = '') AND m.matchDate < CURRENT_TIMESTAMP")
  List<Match> findMatchesWithoutResults(@Param("userId") UUID userId);


  @Query("SELECT DISTINCT m FROM Match m JOIN FETCH m.participants p WHERE p.user.id = :userId AND m.winningTeam IS NOT NULL ORDER BY m.matchDate DESC")
  List<Match> findCompletedMatchesByParticipant(@Param("userId") UUID userId, Pageable pageable);
}
