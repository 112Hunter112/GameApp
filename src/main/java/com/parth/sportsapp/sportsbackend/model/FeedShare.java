package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A user sharing something (currently a match result) onto their friends'
 * feeds — the Instagram/TikTok "share to feed" primitive.
 *
 * Deliberately a REFERENCE, not a copy: the share row carries no content of
 * its own. If the underlying target is deleted, the share hydrates as
 * "unavailable" (the IG 'post unavailable' card) until it is cleaned up.
 * Deleting the share itself removes the row immediately; viewers who already
 * have a cached feed page keep seeing it until their cache TTL expires —
 * source of truth is this table, Redis is only an accelerator.
 */
@Entity
@Table(name = "feed_shares",
    indexes = {
        // The feed query: shares by my friends, newest first.
        @Index(name = "idx_feed_share_user_created", columnList = "user_id, created_at"),
        // Cleanup path when a target (match) is deleted.
        @Index(name = "idx_feed_share_target", columnList = "target_type, target_id")
    },
    uniqueConstraints = {
        // One share of a given thing per user (IG semantics) — enforced by
        // the database, not just the service check.
        @UniqueConstraint(columnNames = {"user_id", "target_type", "target_id"})
    })
public class FeedShare {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false)
  private FeedTargetType targetType;

  @Column(name = "target_id", nullable = false)
  private UUID targetId;

  @Column(length = 280)
  private String caption;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  public FeedShare() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }

  public FeedTargetType getTargetType() { return targetType; }
  public void setTargetType(FeedTargetType targetType) { this.targetType = targetType; }

  public UUID getTargetId() { return targetId; }
  public void setTargetId(UUID targetId) { this.targetId = targetId; }

  public String getCaption() { return caption; }
  public void setCaption(String caption) { this.caption = caption; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
