package com.parth.sportsapp.sportsbackend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What the API returns for an uploaded match image. Uploader is exposed only
 * as id + first name + (initial of last name) so we don't leak the full
 * profile to other match participants.
 */
public class MatchImageResponse {

  private UUID id;
  private String imageUrl;
  private String caption;
  private UUID uploadedById;
  private String uploadedByName;
  private LocalDateTime createdAt;

  public MatchImageResponse() {}

  public MatchImageResponse(UUID id, String imageUrl, String caption,
                            UUID uploadedById, String uploadedByName,
                            LocalDateTime createdAt) {
    this.id = id;
    this.imageUrl = imageUrl;
    this.caption = caption;
    this.uploadedById = uploadedById;
    this.uploadedByName = uploadedByName;
    this.createdAt = createdAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID v) { this.id = v; }

  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String v) { this.imageUrl = v; }

  public String getCaption() { return caption; }
  public void setCaption(String v) { this.caption = v; }

  public UUID getUploadedById() { return uploadedById; }
  public void setUploadedById(UUID v) { this.uploadedById = v; }

  public String getUploadedByName() { return uploadedByName; }
  public void setUploadedByName(String v) { this.uploadedByName = v; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
