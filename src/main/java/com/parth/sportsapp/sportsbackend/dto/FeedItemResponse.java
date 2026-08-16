package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.FeedTargetType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One card in the social feed. When the underlying target has been deleted
 * but the share (or a cached copy of this page) still exists, the item is
 * delivered with {@code unavailable = true} and no summary — the client
 * renders the IG-style "This content is no longer available" card.
 */
public class FeedItemResponse {

  private UUID shareId;
  private UUID sharerId;
  private String sharerName;
  private String sharerAvatarUrl;
  private FeedTargetType targetType;
  private UUID targetId;
  private String caption;
  private LocalDateTime sharedAt;
  private boolean unavailable;

  // MATCH hydration (null when unavailable or a different target type)
  private String score;
  private LocalDateTime matchDate;
  private List<String> participantNames;

  public UUID getShareId() { return shareId; }
  public void setShareId(UUID shareId) { this.shareId = shareId; }

  public UUID getSharerId() { return sharerId; }
  public void setSharerId(UUID sharerId) { this.sharerId = sharerId; }

  public String getSharerName() { return sharerName; }
  public void setSharerName(String sharerName) { this.sharerName = sharerName; }

  public String getSharerAvatarUrl() { return sharerAvatarUrl; }
  public void setSharerAvatarUrl(String sharerAvatarUrl) { this.sharerAvatarUrl = sharerAvatarUrl; }

  public FeedTargetType getTargetType() { return targetType; }
  public void setTargetType(FeedTargetType targetType) { this.targetType = targetType; }

  public UUID getTargetId() { return targetId; }
  public void setTargetId(UUID targetId) { this.targetId = targetId; }

  public String getCaption() { return caption; }
  public void setCaption(String caption) { this.caption = caption; }

  public LocalDateTime getSharedAt() { return sharedAt; }
  public void setSharedAt(LocalDateTime sharedAt) { this.sharedAt = sharedAt; }

  public boolean isUnavailable() { return unavailable; }
  public void setUnavailable(boolean unavailable) { this.unavailable = unavailable; }

  public String getScore() { return score; }
  public void setScore(String score) { this.score = score; }

  public LocalDateTime getMatchDate() { return matchDate; }
  public void setMatchDate(LocalDateTime matchDate) { this.matchDate = matchDate; }

  public List<String> getParticipantNames() { return participantNames; }
  public void setParticipantNames(List<String> participantNames) { this.participantNames = participantNames; }
}
