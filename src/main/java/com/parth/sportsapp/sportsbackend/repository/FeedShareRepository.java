package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.FeedShare;
import com.parth.sportsapp.sportsbackend.model.FeedTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeedShareRepository extends JpaRepository<FeedShare, UUID> {

  /** The feed: shares by me + my friends, newest first, paged. */
  Page<FeedShare> findByUserIdInOrderByCreatedAtDesc(Collection<UUID> userIds, Pageable pageable);

  /** Service-level duplicate guard (the DB unique constraint is the backstop). */
  boolean existsByUserIdAndTargetTypeAndTargetId(UUID userId, FeedTargetType type, UUID targetId);

  /** All shares of one target — used to clean up when the target is deleted. */
  List<FeedShare> findByTargetTypeAndTargetId(FeedTargetType type, UUID targetId);
}
