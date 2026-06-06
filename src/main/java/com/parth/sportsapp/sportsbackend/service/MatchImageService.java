package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.MatchImageRequest;
import com.parth.sportsapp.sportsbackend.dto.MatchImageResponse;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.Match;
import com.parth.sportsapp.sportsbackend.model.MatchImage;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.MatchImageRepository;
import com.parth.sportsapp.sportsbackend.repository.MatchRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Match-image management with multi-layer security.
 *
 * <h3>Threat model</h3>
 * <ul>
 *   <li><b>Auth</b>: only participants can add or list; only the uploader can delete.</li>
 *   <li><b>Mass assignment</b>: {@link MatchImageRequest} exposes only url + caption.</li>
 *   <li><b>URL scheme</b>: reject anything that isn't {@code https://} (no
 *       {@code javascript:}, {@code data:}, {@code file://}, {@code http://}).</li>
 *   <li><b>SSRF defense-in-depth</b>: reject loopback / link-local / RFC-1918
 *       hosts even though we never fetch the URL today — if a future feature
 *       (thumbnailing, moderation) does fetch it, the guard is already in
 *       place. {@code 169.254.169.254} (cloud metadata) is explicitly blocked.</li>
 *   <li><b>Host allow-list</b>: optional, env-driven via
 *       {@code app.match-images.allowed-hosts}. Empty = any non-private https
 *       host is accepted.</li>
 *   <li><b>Storage DoS</b>: hard cap of {@value #MAX_IMAGES_PER_MATCH} per match.</li>
 * </ul>
 */
@Service
public class MatchImageService {

  /** Hard upper bound on images stored per match. Prevents URL-spam DoS. */
  public static final int MAX_IMAGES_PER_MATCH = 20;

  private final MatchImageRepository imageRepository;
  private final MatchRepository matchRepository;
  private final UserRepository userRepository;

  /**
   * Comma-separated allow-list of permitted image hosts.
   * Each entry can be a full hostname ({@code i.imgur.com}) or a bare apex
   * domain ({@code cloudinary.com}) — subdomains of an apex entry are
   * accepted. Empty list means any non-private HTTPS host is allowed.
   */
  private final List<String> allowedHosts;

  public MatchImageService(MatchImageRepository imageRepository,
                           MatchRepository matchRepository,
                           UserRepository userRepository,
                           @Value("${app.match-images.allowed-hosts:}") String allowedHostsRaw) {
    this.imageRepository = imageRepository;
    this.matchRepository = matchRepository;
    this.userRepository = userRepository;
    this.allowedHosts = Arrays.stream(allowedHostsRaw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(String::toLowerCase)
        .toList();
  }

  // --- mutations -----------------------------------------------------------

  @Transactional
  public MatchImageResponse addImage(UUID matchId, UUID userId, MatchImageRequest request) {
    // 1. URL security checks — fail fast before any DB writes.
    validateImageUrl(request.getImageUrl());

    // 2. Authorization — must be a participant in the match.
    Match match = matchRepository.findById(matchId)
        .orElseThrow(() -> new NotFoundException("Match not found"));
    requireParticipant(match, userId);

    // 3. Storage DoS guard.
    if (imageRepository.countByMatch_Id(matchId) >= MAX_IMAGES_PER_MATCH) {
      throw new BadRequestException(
          "Match already has the maximum " + MAX_IMAGES_PER_MATCH + " images");
    }

    // 4. Build entity.
    User uploader = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found"));

    MatchImage img = new MatchImage();
    img.setMatch(match);
    img.setUploadedBy(uploader);
    img.setImageUrl(request.getImageUrl());
    img.setCaption(request.getCaption());

    return toResponse(imageRepository.save(img));
  }

  @Transactional
  public void deleteImage(UUID imageId, UUID userId) {
    MatchImage img = imageRepository.findById(imageId)
        .orElseThrow(() -> new NotFoundException("Image not found"));

    // Only the uploader can delete their own image.
    if (img.getUploadedBy() == null || !img.getUploadedBy().getId().equals(userId)) {
      throw new ForbiddenException("Only the uploader can delete this image");
    }
    imageRepository.delete(img);
  }

  // --- reads ----------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<MatchImageResponse> listImages(UUID matchId, UUID userId) {
    Match match = matchRepository.findById(matchId)
        .orElseThrow(() -> new NotFoundException("Match not found"));
    requireParticipant(match, userId);

    return imageRepository.findByMatch_IdOrderByCreatedAtDesc(matchId).stream()
        .map(this::toResponse)
        .toList();
  }

  // --- authorization helper -------------------------------------------------

  private void requireParticipant(Match match, UUID userId) {
    if (match.getParticipants() == null) {
      throw new ForbiddenException("You are not a participant in this match");
    }
    boolean isParticipant = match.getParticipants().stream()
        .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(userId));
    if (!isParticipant) {
      throw new ForbiddenException("You are not a participant in this match");
    }
  }

  // --- URL security ---------------------------------------------------------

  /**
   * Multi-layer URL validation. Throws {@link BadRequestException} on any
   * failure so the global handler returns a 400 with a generic message.
   */
  private void validateImageUrl(String rawUrl) {
    if (rawUrl == null || rawUrl.isBlank()) {
      throw new BadRequestException("imageUrl is required");
    }
    URI uri;
    try {
      uri = new URI(rawUrl);
    } catch (URISyntaxException e) {
      throw new BadRequestException("Invalid image URL");
    }
    // 1. Scheme: HTTPS only. Blocks javascript:, data:, file://, http://, etc.
    if (uri.getScheme() == null || !"https".equalsIgnoreCase(uri.getScheme())) {
      throw new BadRequestException("imageUrl must use https://");
    }
    // 2. Host required.
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new BadRequestException("imageUrl must include a host");
    }
    String lowerHost = host.toLowerCase();

    // 3. SSRF defense-in-depth: forbid loopback, link-local, RFC-1918 private ranges.
    if (isPrivateOrLocalHost(lowerHost)) {
      throw new BadRequestException("Image host not allowed");
    }

    // 4. Optional host allow-list (env-driven).
    if (!allowedHosts.isEmpty() && !matchesAllowList(lowerHost)) {
      throw new BadRequestException("Image host not on allow-list");
    }
  }

  private boolean matchesAllowList(String lowerHost) {
    for (String allowed : allowedHosts) {
      // exact match or subdomain of an allow-listed apex
      if (lowerHost.equals(allowed) || lowerHost.endsWith("." + allowed)) {
        return true;
      }
    }
    return false;
  }

  /** Crude but pragmatic IP / private-host check. */
  private static boolean isPrivateOrLocalHost(String host) {
    if (host.equals("localhost") || host.equals("0.0.0.0")
        || host.equals("ip6-localhost") || host.equals("ip6-loopback")
        || host.equals("[::1]") || host.equals("::1")) {
      return true;
    }
    if (host.startsWith("127.")) return true;             // loopback
    if (host.startsWith("10.")) return true;              // RFC 1918
    if (host.startsWith("192.168.")) return true;         // RFC 1918
    if (host.startsWith("169.254.")) return true;         // link-local + cloud metadata
    if (host.startsWith("fc") || host.startsWith("fd")) return true; // IPv6 ULA
    if (host.startsWith("fe80:")) return true;            // IPv6 link-local
    if (host.startsWith("172.")) {                        // 172.16.0.0 – 172.31.255.255
      String[] parts = host.split("\\.");
      if (parts.length >= 2) {
        try {
          int second = Integer.parseInt(parts[1]);
          if (second >= 16 && second <= 31) return true;
        } catch (NumberFormatException ignored) { /* not an IP — fine */ }
      }
    }
    return false;
  }

  // --- mapper ---------------------------------------------------------------

  private MatchImageResponse toResponse(MatchImage img) {
    User u = img.getUploadedBy();
    String displayName = null;
    if (u != null) {
      String first = u.getFirstName() == null ? "" : u.getFirstName();
      String lastInitial = (u.getLastName() == null || u.getLastName().isEmpty())
          ? "" : (" " + u.getLastName().charAt(0) + ".");
      displayName = (first + lastInitial).trim();
    }
    return new MatchImageResponse(
        img.getId(),
        img.getImageUrl(),
        img.getCaption(),
        u == null ? null : u.getId(),
        displayName,
        img.getCreatedAt()
    );
  }
}
