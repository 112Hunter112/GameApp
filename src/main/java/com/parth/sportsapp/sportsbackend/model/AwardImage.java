package com.parth.sportsapp.sportsbackend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A photo attached to an Award. We store only the URL — the client uploads
 * bytes directly to Cloudinary and POSTs the resulting URL here.
 */
@Entity
@Table(name = "award_images", indexes = {
    @Index(name = "idx_award_image_award",    columnList = "award_id"),
    @Index(name = "idx_award_image_uploader", columnList = "uploaded_by_user_id")
})
public class AwardImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "award_id", nullable = false)
    private Award award;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "caption", length = 500)
    private String caption;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AwardImage() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Award getAward() { return award; }
    public void setAward(Award award) { this.award = award; }

    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
