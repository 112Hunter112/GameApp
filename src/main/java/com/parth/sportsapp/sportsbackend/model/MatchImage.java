package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One uploaded image attached to a match. We store only the URL — clients
 * upload the bytes directly to S3/Cloudinary/Imgur/etc. and POST the
 * resulting URL here. The backend never holds image bytes in memory.
 */
@Entity
@Table(name = "match_images", indexes = {
    @Index(name = "idx_match_image_match", columnList = "match_id"),
    @Index(name = "idx_match_image_uploader", columnList = "uploaded_by_user_id")
})
public class MatchImage {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "match_id", nullable = false)
  private Match match;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "uploaded_by_user_id", nullable = false)
  private User uploadedBy;

  @Column(name = "image_url", nullable = false, length = 2048)
  private String imageUrl;

  @Column(name = "caption", length = 500)
  private String caption;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public MatchImage() {}

  public UUID getId() { return id; }
  public void setId(UUID v) { this.id = v; }

  public Match getMatch() { return match; }
  public void setMatch(Match v) { this.match = v; }

  public User getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(User v) { this.uploadedBy = v; }

  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String v) { this.imageUrl = v; }

  public String getCaption() { return caption; }
  public void setCaption(String v) { this.caption = v; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
