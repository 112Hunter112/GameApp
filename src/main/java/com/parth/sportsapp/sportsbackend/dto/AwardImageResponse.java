package com.parth.sportsapp.sportsbackend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class AwardImageResponse {

    private UUID id;
    private String imageUrl;
    private String caption;
    private UUID uploadedByUserId;
    private LocalDateTime createdAt;

    public AwardImageResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public UUID getUploadedByUserId() { return uploadedByUserId; }
    public void setUploadedByUserId(UUID uploadedByUserId) { this.uploadedByUserId = uploadedByUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
