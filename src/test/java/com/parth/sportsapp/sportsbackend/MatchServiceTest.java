package com.parth.sportsapp.sportsbackend;

import com.parth.sportsapp.sportsbackend.Validation.MatchValidator;
import com.parth.sportsapp.sportsbackend.dto.ManualMatchRequest;
import com.parth.sportsapp.sportsbackend.dto.MatchResponse;
import com.parth.sportsapp.sportsbackend.mapper.MatchMapper;
import com.parth.sportsapp.sportsbackend.model.*;
import com.parth.sportsapp.sportsbackend.repository.MatchRepository;
import com.parth.sportsapp.sportsbackend.repository.ParticipantsRepository;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import com.parth.sportsapp.sportsbackend.service.EmailService;
import com.parth.sportsapp.sportsbackend.service.MatchService;
import com.parth.sportsapp.sportsbackend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// --- CORRECT IMPORTS BELOW ---
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatchServiceTest {

  @InjectMocks
  private MatchService matchService;

  @Mock private MatchRepository matchRepository;
  @Mock private UserRepository userRepository;
  @Mock private SportsRepository sportsRepository;
  @Mock private ParticipantsRepository participantsRepository;
  @Mock
  private NotificationService notificationService;
  @Mock private MatchValidator matchValidator;
  @Mock private MatchMapper matchMapper;
  @Mock private EmailService emailService;
  // MatchService injects an EntityManager via @PersistenceContext; Mockito does
  // not process that annotation, so we provide an explicit mock for @InjectMocks.
  @Mock private jakarta.persistence.EntityManager entityManager;

@Test
  public void testLogManualMatch_Success() {
  UUID userId = UUID.randomUUID();
  User mockUser = new User();
  mockUser.setId(userId);
  mockUser.setEmail("test@example.com");

  Sports mockSport = new Sports();
  mockSport.setSportName("Tennis");

  ManualMatchRequest request = new ManualMatchRequest();
  request.setSport("Tennis");
  request.setScore("6-0, 6-0");
  request.setWinningTeam("TEAM_A"); // Valid input
  request.setDate(LocalDateTime.now());
  request.setTeammateIds(Collections.emptyList());
  request.setOpponentIds(Collections.emptyList());

  // --- B. DEFINE MOCK BEHAVIOR ---
  // "When the code asks for a sport, return my mockSport object"
  when(sportsRepository.findBySportName("Tennis")).thenReturn(Optional.of(mockSport));

  // "When code asks for user, return my mockUser"
  when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

  // "When saving a match, just return the match that was passed in"
  when(matchRepository.save(any(Match.class))).thenAnswer(i -> i.getArguments()[0]);

  // Mock the mapper to return a non-null response
  when(matchMapper.toDto(any(Match.class))).thenReturn(new MatchResponse());

  // --- C. EXECUTE THE SERVICE ---
  MatchResponse response = matchService.logManualMatch(userId, request);

  // --- D. VERIFY (Assert) ---
  assertNotNull(response);

  // Verify that the repository.save() was actually called once
  verify(matchRepository, times(1)).save(any(Match.class));

  // Verify participants were saved (Creator added as TEAM_A).
  // MatchService persists participants via EntityManager.persist, not the repository.
  verify(entityManager, atLeastOnce()).persist(any(Participants.class));
  }




  @Test
  public void testLogManualMatch_CreatesShadowUser_WhenEmailNotFound() {
    // 1. ARRANGE
    UUID userId = UUID.randomUUID();
    User creator = new User();
    creator.setId(userId);
    creator.setFirstName("Parth");

    Sports sport = new Sports();
    sport.setSportName("Tennis");

    ManualMatchRequest request = new ManualMatchRequest();
    request.setSport("Tennis");
    request.setWinningTeam("TEAM_A");
    request.setScore("6-0");
    request.setDate(LocalDateTime.now());
    // The Input: An email that does NOT exist in the DB
    request.setOpponentEmails(List.of("newguy@gmail.com"));

    when(sportsRepository.findBySportName("Tennis")).thenReturn(Optional.of(sport));
    when(userRepository.findById(userId)).thenReturn(Optional.of(creator));
    // Important: Return Empty when searching for this email
    when(userRepository.findByEmail("newguy@gmail.com")).thenReturn(Optional.empty());

    // Mock saving the NEW user
    when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
    when(matchRepository.save(any(Match.class))).thenAnswer(i -> i.getArguments()[0]);
    when(matchMapper.toDto(any(Match.class))).thenReturn(new MatchResponse());

    // 2. ACT
    matchService.logManualMatch(userId, request);

    // 3. ASSERT
    // Verify that userRepository.save() was called for the NEW user
    verify(userRepository).save(argThat(user ->
        user.getEmail().equals("newguy@gmail.com") &&
            user.getFirstName().equals("Invited") // Check your logic for default name
    ));

    // Verify Email Service was called
    verify(emailService).sendInvite(eq("newguy@gmail.com"), anyString());
  }

  @Test
  public void testVerifyMatch_ThrowsError_IfCreatorTriesToVerify() {
    // 1. ARRANGE
    UUID creatorId = UUID.randomUUID();
    UUID matchId = UUID.randomUUID();

    User creator = new User();
    creator.setId(creatorId);

    Match match = new Match();
    match.setId(matchId);
    match.setCreatedByUser(creator); // Creator is set here

    // Mock Participants (Creator is in the list)
    Participants pCreator = new Participants();
    pCreator.setUser(creator);
    match.setParticipants(List.of(pCreator));

    when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

    // 2. ACT & ASSERT
    // Expect a RuntimeException with specific message
    Exception exception = assertThrows(RuntimeException.class, () -> {
      matchService.verifyMatch(matchId, creatorId, true);
    });

    assertTrue(exception.getMessage().contains("cannot verify your own match"));
  }


  @Test
  public void testVerifyMatch_ThrowsError_IfTeammateTriesToVerify() {
    // 1. ARRANGE
    UUID matchId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID teammateId = UUID.randomUUID();

    User creator = new User(); creator.setId(creatorId);
    User teammate = new User(); teammate.setId(teammateId);

    Match match = new Match();
    match.setId(matchId);
    match.setCreatedByUser(creator);

    // Both are on "TEAM_A"
    Participants pCreator = new Participants();
    pCreator.setUser(creator);
    pCreator.setTeamName("TEAM_A");

    Participants pTeammate = new Participants();
    pTeammate.setUser(teammate);
    pTeammate.setTeamName("TEAM_A"); // Same Team!

    match.setParticipants(List.of(pCreator, pTeammate));

    when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

    // 2. ACT & ASSERT
    Exception exception = assertThrows(RuntimeException.class, () -> {
      matchService.verifyMatch(matchId, teammateId, true);
    });

    // This proves your "Teammate Logic" works
    assertTrue(exception.getMessage().contains("Teammates cannot verify"));
  }


  @Test
  public void testUpdateMatchScore_ResetsStatusToPending() {
    // 1. ARRANGE
    UUID matchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User user = new User(); user.setId(userId);

    Match match = new Match();
    match.setId(matchId);
    match.setVerificationStatus(MatchVerificationStatus.CONFIRMED); // It was verified
    match.setScore("6-0");

    Participants p = new Participants();
    p.setUser(user);
    match.setParticipants(List.of(p));

    when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
    when(matchRepository.save(any(Match.class))).thenAnswer(i -> i.getArguments()[0]);
    when(matchMapper.toDto(any(Match.class))).thenReturn(new MatchResponse());

    // 2. ACT
    matchService.updateMatchScore(matchId, userId, "6-4", "TEAM_A");

    // 3. ASSERT
    // Capture the saved match and check status
    verify(matchRepository).save(argThat(savedMatch ->
        savedMatch.getVerificationStatus() == MatchVerificationStatus.PENDING
    ));
  }
}
