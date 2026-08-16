package com.parth.sportsapp.sportsbackend.dto;

import com.parth.sportsapp.sportsbackend.model.FeedTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** "Share this to my feed." */
public class FeedShareRequest {

  @NotNull(message = "targetType is required")
  private FeedTargetType targetType;

  @NotNull(message = "targetId is required")
  private UUID targetId;

  @Size(max = 280, message = "caption must be at most 280 characters")
  private String caption;

  public FeedTargetType getTargetType() { return targetType; }
  public void setTargetType(FeedTargetType targetType) { this.targetType = targetType; }

  public UUID getTargetId() { return targetId; }
  public void setTargetId(UUID targetId) { this.targetId = targetId; }

  public String getCaption() { return caption; }
  public void setCaption(String caption) { this.caption = caption; }
}
