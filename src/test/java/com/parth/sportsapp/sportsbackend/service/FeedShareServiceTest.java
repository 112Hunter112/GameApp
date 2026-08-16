package com.parth.sportsapp.sportsbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parth.sportsapp.sportsbackend.dto.FeedItemResponse;
import com.parth.sportsapp.sportsbackend.dto.FeedPageResponse;
import com.parth.sportsapp.sportsbackend.dto.FeedShareRequest;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.FeedShare;
import com.parth.sportsapp.sportsbackend.model.FeedTargetType;
import com.parth.sportsapp.sportsbackend.model.Friendship;
import com.parth.sportsapp.sportsbackend.model.Match;
import com.parth.sportsapp.sportsbackend.model.Participants;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.FeedShareRepository;
import com.parth.sportsapp.sportsbackend.repository.FriendshipRepository;
import com.parth.sportsapp.sportsbackend.repository.MatchRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedShareServiceTest {

  private static final long TTL_SECONDS = 120;

  @Mock private FeedShareRepository feedShareRepository;
  @Mock private FriendshipRepository friendshipRepository;
  @Mock private MatchRepository matchRepository;
  @Mock private UserRepository userRepository;
  @Mock private StringRedisTemplate redis;
  @Mock private ValueOperations<String, String> valueOps;

  private FeedShareService service;

  private User me;
  private User friend;

  @BeforeEach
  void setUp() {
    // Not every test touches the value ops — keep the stub lenient.
    lenient().when(redis.opsForValue()).thenReturn(valueOps);
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    service = new FeedShareService(feedShareRepository, friendshipRepository,
        matchRepository, userRepository, redis, mapper, TTL_SECONDS);

    me = user("Parth");
    friend = user("Keshav");
  }

  // --- share -----------------------------------------------------------------

  @Test
  void shareStoresRowAndEvictsOwnCachedPages() {
    Match match = matchWith(me, friend);
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(feedShareRepository.existsByUserIdAndTargetTypeAndTargetId(
        me.getId(), FeedTargetType.MATCH, match.getId())).thenReturn(false);
    when(feedShareRepository.save(any(FeedShare.class))).thenAnswer(inv -> {
      FeedShare s = inv.getArgument(0);
      s.setId(UUID.randomUUID());
      s.setCreatedAt(LocalDateTime.now());
      return s;
    });
    when(userRepository.findById(me.getId())).thenReturn(Optional.of(me));

    FeedItemResponse item = service.share(me.getId(), request(match.getId(), "  gg!  "));

    assertThat(item.getSharerId()).isEqualTo(me.getId());
    assertThat(item.getCaption()).isEqualTo("gg!"); // trimmed
    assertThat(item.isUnavailable()).isFalse();
    assertThat(item.getScore()).isEqualTo("6-4, 6-3");
    assertThat(item.getParticipantNames()).hasSize(2);

    // Sharer's own cached pages must be dropped so they see it immediately.
    verify(redis).delete(argThatContainsKey(me.getId(), 0));
  }

  @Test
  void shareRejectsMatchesTheUserDidNotPlayIn() {
    Match match = matchWith(friend, user("Other"));
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

    assertThatThrownBy(() -> service.share(me.getId(), request(match.getId(), null)))
        .isInstanceOf(ForbiddenException.class);

    verify(feedShareRepository, never()).save(any());
  }

  @Test
  void shareRejectsDuplicates() {
    Match match = matchWith(me, friend);
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(feedShareRepository.existsByUserIdAndTargetTypeAndTargetId(
        me.getId(), FeedTargetType.MATCH, match.getId())).thenReturn(true);

    assertThatThrownBy(() -> service.share(me.getId(), request(match.getId(), null)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("already shared");
  }

  @Test
  void shareRejectsUnknownMatch() {
    UUID matchId = UUID.randomUUID();
    when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.share(me.getId(), request(matchId, null)))
        .isInstanceOf(NotFoundException.class);
  }

  // --- unshare ---------------------------------------------------------------

  @Test
  void unshareDeletesOwnShareAndEvictsOwnCache() {
    FeedShare share = share(me.getId());
    when(feedShareRepository.findById(share.getId())).thenReturn(Optional.of(share));

    service.unshare(share.getId(), me.getId());

    verify(feedShareRepository).delete(share);
    // Only the SHARER's keys are evicted. Friends' cached pages are left to
    // expire via TTL — that lag is the designed IG-like behavior.
    verify(redis).delete(argThatContainsKey(me.getId(), 0));
  }

  @Test
  void unshareRejectsSomeoneElsesShare() {
    FeedShare share = share(friend.getId());
    when(feedShareRepository.findById(share.getId())).thenReturn(Optional.of(share));

    assertThatThrownBy(() -> service.unshare(share.getId(), me.getId()))
        .isInstanceOf(ForbiddenException.class);

    verify(feedShareRepository, never()).delete(any(FeedShare.class));
  }

  // --- feed reads ------------------------------------------------------------

  @Test
  void feedCacheHitSkipsTheDatabaseEntirely() throws Exception {
    FeedPageResponse cached = new FeedPageResponse(List.of(), 0, false);
    when(valueOps.get(FeedShareService.cacheKey(me.getId(), 0)))
        .thenReturn(new ObjectMapper().findAndRegisterModules().writeValueAsString(cached));

    FeedPageResponse res = service.getFeed(me.getId(), 0);

    assertThat(res.getPage()).isZero();
    verifyNoInteractions(feedShareRepository, friendshipRepository);
  }

  @Test
  void feedCacheMissBuildsFromFriendsSharesAndCachesWithTtl() {
    when(valueOps.get(anyString())).thenReturn(null);
    when(friendshipRepository.findAllFriends(me.getId()))
        .thenReturn(List.of(friendship(me, friend)));

    Match match = matchWith(friend, me);
    FeedShare share = share(friend.getId());
    share.setTargetId(match.getId());
    when(feedShareRepository.findByUserIdInOrderByCreatedAtDesc(anyCollection(), any()))
        .thenReturn(new PageImpl<>(List.of(share), PageRequest.of(0, 20), 1));
    when(userRepository.findAllById(any())).thenReturn(List.of(friend));
    when(matchRepository.findAllById(any())).thenReturn(List.of(match));

    FeedPageResponse res = service.getFeed(me.getId(), 0);

    assertThat(res.getItems()).hasSize(1);
    FeedItemResponse item = res.getItems().get(0);
    assertThat(item.getSharerName()).startsWith("Keshav");
    assertThat(item.isUnavailable()).isFalse();

    // The assembled page is cached under the viewer's key with the TTL.
    verify(valueOps).set(eq(FeedShareService.cacheKey(me.getId(), 0)), anyString(),
        eq(Duration.ofSeconds(TTL_SECONDS)));

    // Both directions of the friendship are included as feed authors.
    ArgumentCaptor<Collection<UUID>> authors = ArgumentCaptor.forClass(Collection.class);
    verify(feedShareRepository).findByUserIdInOrderByCreatedAtDesc(authors.capture(), any());
    assertThat(authors.getValue()).contains(me.getId(), friend.getId());
  }

  @Test
  void deletedTargetRendersUnavailableCardInsteadOfVanishing() {
    when(valueOps.get(anyString())).thenReturn(null);
    when(friendshipRepository.findAllFriends(me.getId())).thenReturn(List.of());

    FeedShare share = share(me.getId()); // target match no longer exists
    when(feedShareRepository.findByUserIdInOrderByCreatedAtDesc(anyCollection(), any()))
        .thenReturn(new PageImpl<>(List.of(share), PageRequest.of(0, 20), 1));
    when(userRepository.findAllById(any())).thenReturn(List.of(me));
    when(matchRepository.findAllById(any())).thenReturn(List.of());

    FeedPageResponse res = service.getFeed(me.getId(), 0);

    assertThat(res.getItems()).hasSize(1);
    assertThat(res.getItems().get(0).isUnavailable()).isTrue();
    assertThat(res.getItems().get(0).getScore()).isNull();
  }

  @Test
  void redisFailureFallsBackToDatabase() {
    // Redis down: reads and writes throw; the feed must still be served.
    when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
    when(friendshipRepository.findAllFriends(me.getId())).thenReturn(List.of());
    when(feedShareRepository.findByUserIdInOrderByCreatedAtDesc(anyCollection(), any()))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    when(userRepository.findAllById(any())).thenReturn(List.of());
    when(matchRepository.findAllById(any())).thenReturn(List.of());

    FeedPageResponse res = service.getFeed(me.getId(), 0);

    assertThat(res.getItems()).isEmpty();
    assertThat(res.getPage()).isZero();
  }

  @Test
  void deepPagesAreServedFreshWithoutTouchingTheCache() {
    when(friendshipRepository.findAllFriends(me.getId())).thenReturn(List.of());
    when(feedShareRepository.findByUserIdInOrderByCreatedAtDesc(anyCollection(), any()))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(9, 20), 0));
    when(userRepository.findAllById(any())).thenReturn(List.of());
    when(matchRepository.findAllById(any())).thenReturn(List.of());

    service.getFeed(me.getId(), 9);

    verifyNoInteractions(valueOps);
  }

  // --- target cleanup ----------------------------------------------------------

  @Test
  void targetDeletionRemovesAllSharesAndEvictsEachSharer() {
    UUID matchId = UUID.randomUUID();
    FeedShare mine = share(me.getId());
    FeedShare theirs = share(friend.getId());
    when(feedShareRepository.findByTargetTypeAndTargetId(FeedTargetType.MATCH, matchId))
        .thenReturn(List.of(mine, theirs));

    service.onTargetDeleted(FeedTargetType.MATCH, matchId);

    verify(feedShareRepository).deleteAll(List.of(mine, theirs));
    verify(redis).delete(argThatContainsKey(me.getId(), 0));
    verify(redis).delete(argThatContainsKey(friend.getId(), 0));
  }

  // --- helpers ---------------------------------------------------------------

  private static List<String> argThatContainsKey(UUID userId, int page) {
    return org.mockito.ArgumentMatchers.argThat(keys ->
        keys != null && keys.contains(FeedShareService.cacheKey(userId, page)));
  }

  private FeedShareRequest request(UUID matchId, String caption) {
    FeedShareRequest r = new FeedShareRequest();
    r.setTargetType(FeedTargetType.MATCH);
    r.setTargetId(matchId);
    r.setCaption(caption);
    return r;
  }

  private User user(String first) {
    User u = new User();
    u.setId(UUID.randomUUID());
    u.setFirstName(first);
    u.setLastName("Aditya");
    return u;
  }

  private Match matchWith(User a, User b) {
    Match m = new Match();
    m.setId(UUID.randomUUID());
    m.setScore("6-4, 6-3");
    m.setMatchDate(LocalDateTime.now().minusDays(1));
    Participants pa = new Participants();
    pa.setUser(a);
    Participants pb = new Participants();
    pb.setUser(b);
    m.setParticipants(List.of(pa, pb));
    return m;
  }

  private FeedShare share(UUID sharerId) {
    FeedShare s = new FeedShare();
    s.setId(UUID.randomUUID());
    s.setUserId(sharerId);
    s.setTargetType(FeedTargetType.MATCH);
    s.setTargetId(UUID.randomUUID());
    s.setCaption("gg");
    s.setCreatedAt(LocalDateTime.now());
    return s;
  }

  private Friendship friendship(User requester, User receiver) {
    Friendship f = new Friendship();
    f.setRequester(requester);
    f.setReceiver(receiver);
    return f;
  }
}
