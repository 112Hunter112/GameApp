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
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.FeedShareRepository;
import com.parth.sportsapp.sportsbackend.repository.FriendshipRepository;
import com.parth.sportsapp.sportsbackend.repository.MatchRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The social feed: share a match to your feed, friends see it — the
 * Instagram/TikTok share model, sized for a single-Postgres deployment.
 *
 * Architecture decisions (deliberate, documented for the next reader):
 *
 * FAN-OUT-ON-READ. A share is ONE row; feeds are assembled at read time from
 * friends' shares (IG at small scale, not Twitter's fan-out-on-write). No
 * per-follower copies to keep consistent or clean up.
 *
 * REDIS IS AN ACCELERATOR, NEVER THE TRUTH. Assembled feed pages are cached
 * per viewer for a short TTL. Every entry expires on its own; nothing relies
 * on invalidation reaching every viewer.
 *
 * DELETE SEMANTICS (the "what if someone deletes it" cases):
 *  - Unshare: the row is deleted and the SHARER's cached pages are evicted,
 *    so their own screen is right immediately. Friends who already loaded a
 *    cached page keep seeing the card until their TTL expires (<= cacheTtl),
 *    exactly like IG where a deleted post lingers on an already-open feed.
 *    Tapping through resolves against the DB and shows "unavailable".
 *  - Target (match) deleted: shares pointing at it hydrate as
 *    unavailable=true (the "content no longer available" card). MatchService
 *    additionally calls onTargetDeleted() to remove those shares outright.
 *  - Redis down: every cache op degrades to a no-op and feeds are served
 *    straight from Postgres. Slower, never wrong.
 */
@Service
public class FeedShareService {

  private static final Logger log = LoggerFactory.getLogger(FeedShareService.class);

  /** Only the first pages are cached — deep scrollback is rare and cheap enough. */
  public static final int CACHED_PAGES = 5;
  public static final int PAGE_SIZE = 20;
  private static final String KEY_PREFIX = "feed:v1:";

  private final FeedShareRepository feedShareRepository;
  private final FriendshipRepository friendshipRepository;
  private final MatchRepository matchRepository;
  private final UserRepository userRepository;
  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final Duration cacheTtl;

  public FeedShareService(FeedShareRepository feedShareRepository,
                          FriendshipRepository friendshipRepository,
                          MatchRepository matchRepository,
                          UserRepository userRepository,
                          StringRedisTemplate redis,
                          ObjectMapper objectMapper,
                          @Value("${app.feed.cache-ttl-seconds:120}") long cacheTtlSeconds) {
    this.feedShareRepository = feedShareRepository;
    this.friendshipRepository = friendshipRepository;
    this.matchRepository = matchRepository;
    this.userRepository = userRepository;
    this.redis = redis;
    this.objectMapper = objectMapper;
    this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
  }

  // ==========================================================================
  // SHARE / UNSHARE
  // ==========================================================================

  @Transactional
  public FeedItemResponse share(UUID userId, FeedShareRequest request) {
    if (request.getTargetType() != FeedTargetType.MATCH) {
      throw new BadRequestException("Only matches can be shared right now");
    }

    Match match = matchRepository.findById(request.getTargetId())
        .orElseThrow(() -> new NotFoundException("Match not found"));

    boolean participated = match.getParticipants() != null && match.getParticipants().stream()
        .anyMatch(p -> p.getUser() != null && userId.equals(p.getUser().getId()));
    if (!participated) {
      throw new ForbiddenException("You can only share matches you played in");
    }

    if (feedShareRepository.existsByUserIdAndTargetTypeAndTargetId(
        userId, request.getTargetType(), request.getTargetId())) {
      throw new BadRequestException("You already shared this match");
    }

    FeedShare share = new FeedShare();
    share.setUserId(userId);
    share.setTargetType(request.getTargetType());
    share.setTargetId(request.getTargetId());
    share.setCaption(request.getCaption() == null || request.getCaption().isBlank()
        ? null : request.getCaption().trim());
    FeedShare saved = feedShareRepository.save(share);

    // The sharer must see their own share instantly; friends converge <= TTL.
    evictFeedPages(userId);

    User sharer = userRepository.findById(userId).orElse(null);
    return toItem(saved, sharer, match);
  }

  @Transactional
  public void unshare(UUID shareId, UUID userId) {
    FeedShare share = feedShareRepository.findById(shareId)
        .orElseThrow(() -> new NotFoundException("Share not found"));
    if (!share.getUserId().equals(userId)) {
      // Do not reveal whose share this is.
      throw new ForbiddenException("You cannot delete this share");
    }
    feedShareRepository.delete(share);
    // Own screens update now; already-cached viewer pages age out via TTL.
    evictFeedPages(userId);
  }

  /** Called when a target (e.g. a match) is deleted: remove shares pointing at it. */
  @Transactional
  public void onTargetDeleted(FeedTargetType type, UUID targetId) {
    List<FeedShare> shares = feedShareRepository.findByTargetTypeAndTargetId(type, targetId);
    if (shares.isEmpty()) {
      return;
    }
    feedShareRepository.deleteAll(shares);
    shares.stream().map(FeedShare::getUserId).distinct().forEach(this::evictFeedPages);
  }

  // ==========================================================================
  // FEED (read side)
  // ==========================================================================

  @Transactional(readOnly = true)
  public FeedPageResponse getFeed(UUID userId, int page) {
    int safePage = Math.max(0, page);
    String key = cacheKey(userId, safePage);

    if (safePage < CACHED_PAGES) {
      FeedPageResponse cached = readCache(key);
      if (cached != null) {
        return cached;
      }
    }

    FeedPageResponse fresh = buildFeedPage(userId, safePage);

    if (safePage < CACHED_PAGES) {
      writeCache(key, fresh);
    }
    return fresh;
  }

  private FeedPageResponse buildFeedPage(UUID userId, int page) {
    // Whose shares appear: mine + accepted friends'.
    Set<UUID> authorIds = new HashSet<>();
    authorIds.add(userId);
    for (Friendship f : friendshipRepository.findAllFriends(userId)) {
      authorIds.add(f.getRequester().getId());
      authorIds.add(f.getReceiver().getId());
    }

    Page<FeedShare> shares = feedShareRepository.findByUserIdInOrderByCreatedAtDesc(
        authorIds, PageRequest.of(page, PAGE_SIZE));

    // Batch-hydrate sharers and match targets: two IN queries, no N+1.
    Map<UUID, User> sharers = userRepository
        .findAllById(shares.getContent().stream().map(FeedShare::getUserId).distinct().toList())
        .stream().collect(Collectors.toMap(User::getId, Function.identity()));
    Map<UUID, Match> matches = matchRepository
        .findAllById(shares.getContent().stream()
            .filter(s -> s.getTargetType() == FeedTargetType.MATCH)
            .map(FeedShare::getTargetId).distinct().toList())
        .stream().collect(Collectors.toMap(Match::getId, Function.identity()));

    List<FeedItemResponse> items = new ArrayList<>();
    for (FeedShare share : shares.getContent()) {
      items.add(toItem(share, sharers.get(share.getUserId()), matches.get(share.getTargetId())));
    }
    return new FeedPageResponse(items, page, shares.hasNext());
  }

  private FeedItemResponse toItem(FeedShare share, User sharer, Match match) {
    FeedItemResponse item = new FeedItemResponse();
    item.setShareId(share.getId());
    item.setSharerId(share.getUserId());
    if (sharer != null) {
      item.setSharerName(sharer.getFirstName() + " " + sharer.getLastName());
      item.setSharerAvatarUrl(sharer.getProfilePictureUrl());
    }
    item.setTargetType(share.getTargetType());
    item.setTargetId(share.getTargetId());
    item.setCaption(share.getCaption());
    item.setSharedAt(share.getCreatedAt());

    if (match == null) {
      // Target deleted (or not visible): the "content unavailable" card.
      item.setUnavailable(true);
      return item;
    }
    item.setUnavailable(false);
    item.setScore(match.getScore());
    item.setMatchDate(match.getMatchDate());
    if (match.getParticipants() != null) {
      item.setParticipantNames(match.getParticipants().stream()
          .filter(p -> p.getUser() != null)
          .map(p -> p.getUser().getFirstName() + " " + p.getUser().getLastName())
          .toList());
    }
    return item;
  }

  // ==========================================================================
  // CACHE PLUMBING — best effort by design; Postgres remains the truth.
  // ==========================================================================

  /** Key layout is part of the operational contract (debugging, evictions). */
  public static String cacheKey(UUID userId, int page) {
    return KEY_PREFIX + userId + ":p" + page;
  }

  private FeedPageResponse readCache(String key) {
    try {
      String json = redis.opsForValue().get(key);
      return json == null ? null : objectMapper.readValue(json, FeedPageResponse.class);
    } catch (Exception e) {
      log.warn("Feed cache read failed ({}); serving from DB", e.getMessage());
      return null;
    }
  }

  private void writeCache(String key, FeedPageResponse page) {
    try {
      redis.opsForValue().set(key, objectMapper.writeValueAsString(page), cacheTtl);
    } catch (Exception e) {
      log.warn("Feed cache write failed ({}); continuing uncached", e.getMessage());
    }
  }

  private void evictFeedPages(UUID userId) {
    try {
      List<String> keys = new ArrayList<>();
      for (int p = 0; p < CACHED_PAGES; p++) {
        keys.add(cacheKey(userId, p));
      }
      redis.delete(keys);
    } catch (Exception e) {
      log.warn("Feed cache evict failed ({}); entries will expire via TTL", e.getMessage());
    }
  }
}
