package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/matches/{id}/images.
 *
 * <p>Intentionally minimal: the client supplies ONLY the public image URL and
 * an optional caption. {@code uploadedBy}, {@code match}, {@code createdAt}
 * are filled in server-side from the JWT and path — never trusted from the
 * client.</p>
 *
 * <p>The {@code @Pattern} only does a cheap early-reject. Real URL validation
 * (scheme allow-list, SSRF guard, host allow-list) happens server-side in
 * {@code MatchImageService}.</p>
 */
public class MatchImageRequest {

  @NotBlank(message = "imageUrl is required")
  @Size(max = 2048, message = "imageUrl too long")
  @Pattern(regexp = "^https://.+", message = "imageUrl must use https://")
  private String imageUrl;

  @Size(max = 500, message = "caption cannot exceed 500 characters")
  private String caption;

  public MatchImageRequest() {}

  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String v) { this.imageUrl = v; }

  public String getCaption() { return caption; }
  public void setCaption(String v) { this.caption = v; }
}
