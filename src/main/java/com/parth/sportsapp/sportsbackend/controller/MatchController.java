package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.*;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import com.parth.sportsapp.sportsbackend.service.JwtUtil;
import com.parth.sportsapp.sportsbackend.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
// 🔒 GLOBAL SECURITY: Only logged-in users can access ANY match endpoint
@PreAuthorize("isAuthenticated()")
public class MatchController {

  @Autowired
  private MatchService matchService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtUtil jwtUtil;

  // ============================================
  // 1. MATCH LOGGING & HISTORY
  // ============================================

  /**
   * Log a manual match result (Tennis, Soccer, etc.)
   */
  @PostMapping("/manual")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Example: Restricting specific roles
  public ResponseEntity<MatchResponse> logManualMatch(@RequestBody ManualMatchRequest request) {
    // We get the ID from the JWT, so users can't fake being someone else
    return ResponseEntity.ok(matchService.logManualMatch(getCurrentUserId(), request));
  }

  /**
   * Get Match History (Paged)
   */
  @GetMapping
  public ResponseEntity<Page<MatchResponse>> getMatchHistory(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(matchService.allMatches(getCurrentUserId(), pageable));
  }

  // ============================================
  // 2. DASHBOARD (HOME SCREEN)
  // ============================================

  @GetMapping("/upcoming")
  public ResponseEntity<List<MatchResponse>> getUpcomingMatches() {
    return ResponseEntity.ok(matchService.getUpcomingMatches(getCurrentUserId()));
  }

  @GetMapping("/today")
  public ResponseEntity<List<MatchResponse>> getMatchesToday() {
    return ResponseEntity.ok(matchService.getMatchesToday(getCurrentUserId()));
  }

  @GetMapping("/pending-verifications")
  public ResponseEntity<List<MatchResponse>> getPendingVerifications() {
    return ResponseEntity.ok(matchService.getPendingVerifications(getCurrentUserId()));
  }

  @GetMapping("/incomplete")
  public ResponseEntity<List<MatchResponse>> getMatchesWithoutResults() {
    return ResponseEntity.ok(matchService.getMatchesWithoutResults(getCurrentUserId()));
  }

  // ============================================
  // 3. STATISTICS (PROFILE)
  // ============================================

  /**
   * THIS WORKS
   *
   * @return
   */
  @GetMapping("/stats/overall")
  public ResponseEntity<UserStatsDto> getOverallStats() {
    return ResponseEntity.ok(matchService.getOverallStats(getCurrentUserId()));
  }

  @GetMapping("/stats/monthly")
  public ResponseEntity<MonthlyStatsDto> getMonthlyStats() {
    return ResponseEntity.ok(matchService.getMonthlyStats(getCurrentUserId()));
  }

  @GetMapping("/stats/streak")
  public ResponseEntity<StreakDto> getCurrentStreak(@RequestHeader("Authorization") String authHeader) {
    // 1. Extract UUID
    UUID userId = getUserIdFromToken(authHeader);

    // 2. Get the streak count from Service
    Integer streakCount = matchService.getCurrentStreak(userId);

    // 3. Determine the Label
    // Since your current service logic ONLY counts wins, we know:
    // If > 0, it's a WIN streak. If 0, it's NONE.
    String streakType = (streakCount > 0) ? "WIN" : "NONE";

    // 4. Return result with the correct label
    return ResponseEntity.ok(new StreakDto(streakType, streakCount));
  }

  // Your helper method
  private UUID getUserIdFromToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new RuntimeException("Invalid Authorization Header");
    }
    String jwtToken = authHeader.substring(7);

    // Ensure your JwtUtil has this method returning a String UUID
    String userIdString = jwtUtil.extractUserId(jwtToken);

    return UUID.fromString(userIdString);
  }

  // ============================================
  // 4. SOCIAL & RIVALS
  // ============================================

  @GetMapping("/head-to-head/{opponentId}")
  public ResponseEntity<HeadToHeadDto> getHeadToHead(@PathVariable UUID opponentId) {
    return ResponseEntity.ok(matchService.getHeadToHead(getCurrentUserId(), opponentId));
  }

  @GetMapping("/opponents/most-played")
  public ResponseEntity<List<OpponentStatsDto>> getMostPlayedOpponents(
      @RequestParam(defaultValue = "5") int limit) {
    return ResponseEntity.ok(matchService.getMostPlayedOpponents(getCurrentUserId(), limit));
  }

  // ============================================
  // 5. ACTIONS (VERIFY / UPDATE)
  // ============================================

  /**
   * Verify a match result
   * Use query param: ?approve=true OR ?approve=false
   */
  @PostMapping("/{matchId}/verify")
  public ResponseEntity<MatchResponse> verifyMatch(
      @PathVariable UUID matchId,
      @RequestParam boolean approve) {
    return ResponseEntity.ok(matchService.verifyMatch(matchId, getCurrentUserId(), approve));
  }

  /**
   * Update Score for an existing match
   */
  @PutMapping("/{matchId}/score")
  public ResponseEntity<MatchResponse> updateMatchScore(
      @PathVariable UUID matchId,
      @RequestBody UpdateScoreRequest request) {
    return ResponseEntity.ok(matchService.updateMatchScore(
        matchId,
        getCurrentUserId(),
        request.getScore(),
        request.getWinningTeam()
    ));
  }

  // ============================================
  // 🔐 HELPER: EXTRACT USER ID FROM JWT
  // ============================================

  private UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new RuntimeException("User not authenticated");
    }

    Object principal = authentication.getPrincipal();

    // 1. Best Case: It's already your User Entity
    if (principal instanceof User) {
      return ((User) principal).getId();
    }

    // 2. Fallback: It's a standard UserDetails or a String (Email)
    String email;
    if (principal instanceof UserDetails) {
      email = ((UserDetails) principal).getUsername();
    } else if (principal instanceof String) {
      email = (String) principal;
    } else {
      throw new RuntimeException("Unknown principal type: " + principal.getClass().getName());
    }

    // 3. Database Lookup using the email
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found for email: " + email))
        .getId();
  }
}
