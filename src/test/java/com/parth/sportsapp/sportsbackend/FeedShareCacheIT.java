package com.parth.sportsapp.sportsbackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parth.sportsapp.sportsbackend.dto.FeedPageResponse;
import com.parth.sportsapp.sportsbackend.model.FeedShare;
import com.parth.sportsapp.sportsbackend.model.FeedTargetType;
import com.parth.sportsapp.sportsbackend.model.Friendship;
import com.parth.sportsapp.sportsbackend.model.Match;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.FeedShareRepository;
import com.parth.sportsapp.sportsbackend.repository.FriendshipRepository;
import com.parth.sportsapp.sportsbackend.repository.MatchRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import com.parth.sportsapp.sportsbackend.service.FeedShareService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves the feed's Instagram-style freshness semantics against a REAL Redis
 * with a REAL TTL (repositories are mocked; Redis is not):
 *
 *   1. A friend's share appears in the viewer's feed and the page is cached.
 *   2. The sharer deletes it: the sharer's own view updates immediately,
 *      but the viewer's already-cached page STILL shows the item.
 *   3. When the TTL expires, the viewer's feed converges — the item is gone.
 *
 * Uses Testcontainers (redis:7-alpine). Set FEED_IT_REDIS_HOST/PORT to reuse
 * an external Redis instead (e.g. environments without Docker).
 */
class FeedShareCacheIT {

  private static final long TTL_SECONDS = 2;

  private static GenericContainer<?> container;
  private static LettuceConnectionFactory connectionFactory;
  private static StringRedisTemplate redis;

  private FeedShareRepository feedShareRepository;
  private FriendshipRepository friendshipRepository;
  private MatchRepository matchRepository;
  private UserRepository userRepository;
  private FeedShareService service;

  private final List<FeedShare> store = new ArrayList<>(); // stand-in for the DB table

  private User sharer;
  private User viewer;
  private Match match;

  @BeforeAll
  static void startRedis() {
    String externalHost = System.getenv("FEED_IT_REDIS_HOST");
    String host;
    int port;
    if (externalHost != null && !externalHost.isBlank()) {
      host = externalHost;
      port = Integer.parseInt(System.getenv().getOrDefault("FEED_IT_REDIS_PORT", "6379"));
    } else {
      container = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
          .withExposedPorts(6379);
      container.start();
      host = container.getHost();
      port = container.getMappedPort(6379);
    }
    connectionFactory = new LettuceConnectionFactory(host, port);
    connectionFactory.afterPropertiesSet();
    redis = new StringRedisTemplate(connectionFactory);
    redis.afterPropertiesSet();
  }

  @AfterAll
  static void stopRedis() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
    if (container != null) {
      container.stop();
    }
  }

  @BeforeEach
  void setUp() {
    feedShareRepository = mock(FeedShareRepository.class);
    friendshipRepository = mock(FriendshipRepository.class);
    matchRepository = mock(MatchRepository.class);
    userRepository = mock(UserRepository.class);

    service = new FeedShareService(feedShareRepository, friendshipRepository,
        matchRepository, userRepository, redis,
        new ObjectMapper().findAndRegisterModules(), TTL_SECONDS);

    sharer = user("Keshav");
    viewer = user("Parth");
    match = new Match();
    match.setId(UUID.randomUUID());
    match.setScore("6-4, 6-3");
    match.setMatchDate(LocalDateTime.now().minusDays(1));
    match.setParticipants(List.of());

    store.clear();

    // Friendship both ways; the "DB" is the mutable store list.
    Friendship friendship = new Friendship();
    friendship.setRequester(sharer);
    friendship.setReceiver(viewer);
    when(friendshipRepository.findAllFriends(any())).thenReturn(List.of(friendship));
    when(feedShareRepository.findByUserIdInOrderByCreatedAtDesc(anyCollection(), any()))
        .thenAnswer(inv -> new PageImpl<>(List.copyOf(store), PageRequest.of(0, 20), store.size()));
    when(userRepository.findAllById(any())).thenReturn(List.of(sharer, viewer));
    when(matchRepository.findAllById(any())).thenReturn(List.of(match));
    doAnswer(inv -> {
      store.remove((FeedShare) inv.getArgument(0));
      return null;
    }).when(feedShareRepository).delete(any(FeedShare.class));

    // A fresh cache per test — evict both users' pages.
    for (int p = 0; p < FeedShareService.CACHED_PAGES; p++) {
      redis.delete(FeedShareService.cacheKey(sharer.getId(), p));
      redis.delete(FeedShareService.cacheKey(viewer.getId(), p));
    }
  }

  @Test
  void deletedShareLingersOnlyForViewersWithAWarmCacheUntilTtlExpiry() throws Exception {
    FeedShare share = newShare();
    store.add(share);
    when(feedShareRepository.findById(share.getId())).thenReturn(Optional.of(share));

    // 1. Viewer loads their feed: the friend's share is there, page now cached.
    assertThat(service.getFeed(viewer.getId(), 0).getItems()).hasSize(1);

    // 2. Sharer deletes the share (row gone, sharer's cache evicted).
    service.unshare(share.getId(), sharer.getId());
    assertThat(service.getFeed(sharer.getId(), 0).getItems()).isEmpty();

    // 3. Viewer's WARM cache still shows the deleted item — the IG lag.
    assertThat(service.getFeed(viewer.getId(), 0).getItems()).hasSize(1);

    // 4. After the TTL, the viewer converges to the truth: it's gone.
    TimeUnit.MILLISECONDS.sleep(TTL_SECONDS * 1000 + 400);
    assertThat(service.getFeed(viewer.getId(), 0).getItems()).isEmpty();
  }

  @Test
  void coldViewersNeverSeeADeletedShare() {
    FeedShare share = newShare();
    store.add(share);
    when(feedShareRepository.findById(share.getId())).thenReturn(Optional.of(share));

    // Deleted BEFORE the viewer ever loaded: nothing cached, nothing shown.
    service.unshare(share.getId(), sharer.getId());

    assertThat(service.getFeed(viewer.getId(), 0).getItems()).isEmpty();
  }

  @Test
  void cachedFeedPagesCarryTheConfiguredTtl() {
    store.add(newShare());

    service.getFeed(viewer.getId(), 0);

    Long expiry = redis.getExpire(FeedShareService.cacheKey(viewer.getId(), 0), TimeUnit.MILLISECONDS);
    assertThat(expiry).isNotNull().isGreaterThan(0).isLessThanOrEqualTo(TTL_SECONDS * 1000);
  }

  // --- helpers ---------------------------------------------------------------

  private FeedShare newShare() {
    FeedShare s = new FeedShare();
    s.setId(UUID.randomUUID());
    s.setUserId(sharer.getId());
    s.setTargetType(FeedTargetType.MATCH);
    s.setTargetId(match.getId());
    s.setCaption("what a rally");
    s.setCreatedAt(LocalDateTime.now());
    return s;
  }

  private User user(String first) {
    User u = new User();
    u.setId(UUID.randomUUID());
    u.setFirstName(first);
    u.setLastName("Aditya");
    return u;
  }
}
