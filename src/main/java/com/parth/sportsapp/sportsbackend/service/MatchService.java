package com.parth.sportsapp.sportsbackend.service;


import com.parth.sportsapp.sportsbackend.Validation.MatchValidator;
import com.parth.sportsapp.sportsbackend.dto.*;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.mapper.MatchMapper;
import com.parth.sportsapp.sportsbackend.model.*;

import com.parth.sportsapp.sportsbackend.repository.MatchRepository;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import com.parth.sportsapp.sportsbackend.repository.ParticipantsRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MatchService {

  @Autowired MatchRepository matchRepository;
  @Autowired MatchMapper matchMapper;
  @Autowired UserRepository userRepository;
  @Autowired EmailService emailService;
  @Autowired ParticipantsRepository participantsRepository;
  @Autowired NotificationService notificationService;
  @Autowired MatchValidator matchValidator;
  @Autowired SportsRepository sportsRepository;
  @PersistenceContext EntityManager entityManager;



  /**
   * // list all matches to the User is a pageable format, for the profile page
   *
   * The userID is from the JWT
   * @param userID
   * @param pageable
   * @return
   */
  @Transactional(readOnly = true)
  public Page<MatchResponse> allMatches(UUID userID, Pageable pageable) {

    Page<Match> matchPage = matchRepository.findHistory(userID, pageable);


    return matchPage.map(m -> matchMapper.toDto(m));// how am i supposed to convert the
    // matchPage into
  }



  /**
   * Allow users to add matches manually, set MatchSource as Personal
   * @param userId This is is the usersUUID so we can track who it is
   * @param manualMatchRequest the input the user fills out so we can add him
   */
  @Transactional
  public MatchResponse logManualMatch(UUID userId, ManualMatchRequest manualMatchRequest) {

// find the sport the user entered
    Sports sportEntity = sportsRepository.findBySportName(manualMatchRequest.getSport())
        .orElseThrow(() -> new IllegalArgumentException("Sport not found: " + manualMatchRequest.getSport()));

    //validate the info is corrct from the Users end
    matchValidator.validate(manualMatchRequest, sportEntity, userId);

    // Fetch Creator, the User, use JWT
    User creator = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("Current user not found"));

    // Create and Setup Match
    Match match = new Match();
    match.setCreatedByUser(creator); // set creator
    match.setMatchDate(manualMatchRequest.getDate()); // set date
    match.setScore(manualMatchRequest.getScore()); // set score
    match.setDescription(manualMatchRequest.getNotes());
    match.setSport(sportEntity);

    match.setSource(MatchSource.MANUAL_ENTRY);
    match.setBooking(null);
    match.setVerificationStatus(MatchVerificationStatus.PENDING);
    match.setWinningTeam(manualMatchRequest.getWinningTeam());


    // save the info as an entity and push to DB
    Match savedMatch = matchRepository.save(match);

    //save all participants in the participants table

    //add the creator and set status as accepted for participation status
    addParticipant(savedMatch, creator, true, ParticipationStatus.ACCEPTED, "TEAM_A");

    //add teammates as TEAM_A, iterate through the list of teammates and add them to participants
    // as PENDING
    for (UUID teammateId : manualMatchRequest.getTeammateIds()) {

      User teammate = userRepository.findById(teammateId)
          .orElseThrow(() -> new NotFoundException("Teammate not found"));

      addParticipant(savedMatch, teammate, false, ParticipationStatus.PENDING, "TEAM_A");

      notificationService.sendMatchInvite(teammate, creator, savedMatch.getId());
    }
    // once the info is in the db we now send the info for Notification to all other users


    for (UUID oppId : manualMatchRequest.getOpponentIds()) {
      User opponent = userRepository.findById(oppId)
          .orElseThrow(() -> new NotFoundException("Opponent not found"));

      // 1. Save to DB
      addParticipant(savedMatch, opponent, false, ParticipationStatus.PENDING, "TEAM_B");

      // 2. Send Alert 🔔
      notificationService.sendMatchInvite(opponent, creator, savedMatch.getId());
    }

    if (manualMatchRequest.getOpponentEmails() != null) {
      for (String email : manualMatchRequest.getOpponentEmails()) {

        // Check if user exists, otherwise create a "Shadow User"
        User participantUser = userRepository.findByEmail(email).orElse(null);

        if (participantUser == null) {
          // Create Shadow User so we can add them to Participants table
          participantUser = new User();
          participantUser.setEmail(email);
          participantUser.setFirstName("Invited");
          participantUser.setLastName("Player");
          // Fill mandatory fields with random data to pass @NotBlank validation
          participantUser.setPassword(UUID.randomUUID().toString());
          participantUser.setPhoneNumber("INVITE-" + UUID.randomUUID().toString());
          participantUser.setRole(UserRole.USER);
          participantUser.setVerified(false);

          participantUser = userRepository.save(participantUser); // Save to DB

          // Send Email Invite 📧
          emailService.sendInvite(email, creator.getFirstName());
        } else {
          // Only send in-app notification if they already exist
          notificationService.sendMatchInvite(participantUser, creator, savedMatch.getId());
        }

        // Add to Match (Now safe for multiple emails!)
        addParticipant(savedMatch, participantUser, false, ParticipationStatus.PENDING, "TEAM_B");
      }
    }


    return matchMapper.toDto(savedMatch);
  }

  /**
   *
   * Save the Match, the User to whom it belongs and Status and TeamName
   *
   */
  private void addParticipant(Match match, User user, boolean isHost, ParticipationStatus status, String teamName) {
    Participants p = new Participants();
    ParticipantsId pid = new ParticipantsId(match.getId(), user.getId());

    p.setId(pid);
    p.setMatch(match);
    p.setUser(user);
    p.setHost(isHost);
    p.setStatus(status); // Now valid: passing Enum to Enum
    p.setTeamName(teamName);

    entityManager.persist(p);
  }

  /**
   * THIS WORKS
   *
   * @param userId
   * @return
   */
  public UserStatsDto getOverallStats(UUID userId) {
    long totalMatches = matchRepository.countTotalMatches(userId);
    long wins = matchRepository.countTotalWins(userId);
    long losses = totalMatches - wins;
    double winRate = totalMatches > 0 ? (wins * 100.0 / totalMatches) : 0.0;

    return new UserStatsDto(
        totalMatches,
        wins,
        losses,
        Math.round(winRate * 10.0) / 10.0
    );
  }


  /**
   * This returns if you won or lost by checking which team you were on and comparing to the list
   * of members on the winning team
   *
   * @param userId
   * @param match
   * @return
   */
  private boolean isUserOnWinningTeam(UUID userId, Match match) {
    if (match.getWinningTeam() == null) return false;

    String winningTeamStr = match.getWinningTeam();

    return match.getParticipants().stream()
        .filter(p -> p.getUser().getId().equals(userId))
        .anyMatch(p -> p.getTeamName().equals(winningTeamStr));
  }

  @Transactional(readOnly = true)
  public MonthlyStatsDto getMonthlyStats(UUID userId) {
    LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
    LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

    List<Match> monthMatches = matchRepository.findMatchesThisMonth(userId, startOfMonth, endOfMonth);

    // todo : This is the issue in the  code
    // this finds how many wins th user has
    long wins = monthMatches.stream().filter(m -> isUserOnWinningTeam(userId, m)).count();

    long totalMatches = monthMatches.size();
    long losses = totalMatches - wins;
    double winRate = totalMatches > 0 ? (wins * 100.0 / totalMatches) : 0.0;

    return new MonthlyStatsDto(
        totalMatches, wins, losses,
        Math.round(winRate * 10.0) / 10.0,
        LocalDateTime.now().getMonth().toString()
    );
  }

  @Transactional(readOnly = true)
  public Integer getCurrentStreak(UUID userId) { // <--- Changed signature to accept ID
    // 1. Logic removed: No more SecurityContext lookup here.

    // 2. Fetch matches using the passed ID directly
    List<Match> userMatches = matchRepository.findCompletedMatchesByParticipant(
        userId,
        PageRequest.of(0, 100)
    );

    int streak = 0;
    for (Match match : userMatches) {
      // Pass the userId parameter
      if (isUserOnWinningTeam(userId, match)) {
        streak++;
      } else {
        break;
      }
    }
    return streak;
  }

  // ============================================
  // 3. DASHBOARD METHODS
  // ============================================

  public List<MatchResponse> getUpcomingMatches(UUID userId) {
    return matchRepository.findUpcomingMatches(userId).stream()
        .map(matchMapper::toDto)
        .collect(Collectors.toList());
  }

  public List<MatchResponse> getMatchesToday(UUID userId) {
    // Calculate Start (00:00) and End (23:59) of today
    LocalDateTime start = LocalDateTime.now().with(java.time.LocalTime.MIN);
    LocalDateTime end = LocalDateTime.now().with(java.time.LocalTime.MAX);

    // Pass them to the repository
    List<Match> matches = matchRepository.findMatchesToday(userId, start, end);

    return matches.stream()
        .map(matchMapper::toDto)
        .collect(Collectors.toList());
  }

  public List<MatchResponse> getPendingVerifications(UUID userId) {
    return matchRepository.findPendingVerifications(userId).stream()
        .map(matchMapper::toDto)
        .collect(Collectors.toList());
  }

  public List<MatchResponse> getMatchesWithoutResults(UUID userId) {
    return matchRepository.findMatchesWithoutResults(userId).stream()
        .map(matchMapper::toDto)
        .collect(Collectors.toList());
  }

  // ============================================
  // 4. SOCIAL METHODS
  // ============================================
  @Transactional(readOnly = true)
  public HeadToHeadDto getHeadToHead(UUID myId, UUID opponentId) {
    List<Match> matches = matchRepository.findHeadToHead(myId, opponentId);

    long myWins = matches.stream().filter(m -> isUserOnWinningTeam(myId, m)).count();
    long opponentWins = matches.stream().filter(m -> isUserOnWinningTeam(opponentId, m)).count();

    Match mostRecent = matches.isEmpty() ? null : matches.get(0);
    User opponent = userRepository.findById(opponentId).orElseThrow();

    return new HeadToHeadDto(
        matches.size(), myWins, opponentWins,
        mostRecent != null ? matchMapper.toDto(mostRecent) : null,
        opponent.getFirstName() + " " + opponent.getLastName()
    );
  }
  @Transactional(readOnly = true)
  public List<OpponentStatsDto> getMostPlayedOpponents(UUID userId, int limit) {
    List<Object[]> results = matchRepository.findMostPlayedOpponents(userId, PageRequest.of(0, limit));
    List<OpponentStatsDto> stats = new ArrayList<>();

    for (Object[] result : results) {
      User opponent = (User) result[0];
      Long matchCount = (Long) result[1];

      List<Match> headToHead = matchRepository.findHeadToHead(userId, opponent.getId());
      long wins = headToHead.stream().filter(m -> isUserOnWinningTeam(userId, m)).count();
      long losses = matchCount - wins;

      stats.add(new OpponentStatsDto(
          opponent.getId(),
          opponent.getFirstName() + " " + opponent.getLastName(),
          matchCount, wins, losses
      ));
    }
    return stats;
  }

  // ============================================
  // 5. VERIFICATION LOGIC
  // ============================================

  @Transactional
  public void cancelMatch(UUID matchId, UUID userId) {
    Match match = matchRepository.findById(matchId)
        .orElseThrow(() -> new NotFoundException("Match not found"));

    if (!match.getCreatedByUser().getId().equals(userId)) {
      throw new ForbiddenException("Only the person who logged this match can cancel it");
    }

    if (match.getVerificationStatus() != MatchVerificationStatus.PENDING) {
      throw new BadRequestException("Only pending matches can be cancelled");
    }

    participantsRepository.deleteAll(match.getParticipants());
    matchRepository.delete(match);
  }

  public MatchResponse verifyMatch(UUID matchId, UUID userId, boolean approve) {
    Match match = matchRepository.findById(matchId)
        .orElseThrow(() -> new NotFoundException("Match not found"));

    // 1. Find the Verifier (The user trying to click the button)
    Participants verifier = match.getParticipants().stream()
        .filter(p -> p.getUser().getId().equals(userId))
        .findFirst()
        .orElseThrow(() -> new ForbiddenException("You are not a participant in this match"));

    // 2. Find the Creator (The one who logged the match)
    Participants creator = match.getParticipants().stream()
        .filter(p -> p.getUser().getId().equals(match.getCreatedByUser().getId()))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Creator not found in participants"));

    // 3. SECURITY CHECK: Prevent Creator from verifying
    if (match.getCreatedByUser().getId().equals(userId)) {
      throw new ForbiddenException("You cannot verify your own match submission");
    }

    // 4. SECURITY CHECK: Prevent Teammates from verifying [THE FIX]
    // If Verifier is on "TEAM_A" and Creator is on "TEAM_A", block it.
    if (verifier.getTeamName().equals(creator.getTeamName())) {
      throw new ForbiddenException("Teammates cannot verify match results. Please ask an opponent to verify.");
    }

    // 5. Process Verification
    if (approve) {
      match.setVerificationStatus(MatchVerificationStatus.CONFIRMED);

      // Update the verifier's status to ACCEPTED
      verifier.setStatus(ParticipationStatus.ACCEPTED);
      participantsRepository.save(verifier);

      // TODO(elo): when the match transitions to CONFIRMED for the first time
      // (check !match.isRatingsApplied()), compute the new Elo for every
      // participant and set match.setRatingsApplied(true) so re-confirmation
      // doesn't double-apply. The repo methods you need already exist:
      //   userPreferenceRepository.findByUser_IdAndSports_Id(userId, sportId)
      //   userPreferenceRepository.save(...)

      // Notify the creator
      notificationService.sendMatchVerified(match.getCreatedByUser(), match.getId());
    } else {
      match.setVerificationStatus(MatchVerificationStatus.REJECTED);

      // Notify the creator
      notificationService.sendMatchRejected(match.getCreatedByUser(), match.getId());
    }

    return matchMapper.toDto(matchRepository.save(match));
  }

  @Transactional
  public MatchResponse updateMatchScore(UUID matchId, UUID userId, String score, String winningTeam) {
    Match match = matchRepository.findById(matchId)
        .orElseThrow(() -> new NotFoundException("Match not found"));

    boolean isParticipant = match.getParticipants().stream()
        .anyMatch(p -> p.getUser().getId().equals(userId));

    if (!isParticipant) throw new ForbiddenException("You are not a participant in this match");

    match.setScore(score);
    match.setWinningTeam(winningTeam);

    if (match.getVerificationStatus() == MatchVerificationStatus.CONFIRMED) {
      match.setVerificationStatus(MatchVerificationStatus.PENDING);
      // Notify everyone else
      match.getParticipants().stream()
          .filter(p -> !p.getUser().getId().equals(userId))
          .forEach(p -> notificationService.sendScoreUpdated(p.getUser(), match.getId()));
    }

    return matchMapper.toDto(matchRepository.save(match));
  }

}
