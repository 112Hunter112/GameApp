package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/me/awards/{awardId}/images.
 * Client uploads to Cloudinary first, then POSTs the resulting URL here.
 */
public class AwardImageRequest {

    @NotBlank(message = "imageUrl is required")
    @Size(max = 2048, message = "imageUrl too long")
    @Pattern(
        regexp = "^https://.*",
        message = "imageUrl must be an https:// URL"
    )
    private String imageUrl;

    @Size(max = 500, message = "caption must be 500 characters or fewer")
    private String caption;

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
}
