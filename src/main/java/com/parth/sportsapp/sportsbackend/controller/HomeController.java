package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.HomeFeedResponse;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.service.HomeFeedService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The home screen's one endpoint.
 *
 * <p>Returns the user's preferences plus venues near their GPS that host courts
 * for those preferred sports. Optional query params let the frontend narrow the
 * view (single sport, override radius, change result count).</p>
 */
@RestController
@RequestMapping("/api/me/home")
@Validated
@PreAuthorize("isAuthenticated()")
public class HomeController {

  private final HomeFeedService homeFeedService;
  private final com.parth.sportsapp.sportsbackend.service.UserService userService;

  public HomeController(HomeFeedService homeFeedService,
                        com.parth.sportsapp.sportsbackend.service.UserService userService) {
    this.homeFeedService = homeFeedService;
    this.userService = userService;
  }

  @GetMapping
  public HomeFeedResponse home(
      @AuthenticationPrincipal User user,
      @RequestParam @Min(-90)  @Max(90)  double latitude,
      @RequestParam @Min(-180) @Max(180) double longitude,
      @RequestParam(required = false) Double radiusMeters,
      @RequestParam(required = false) UUID sportId,
      @RequestParam(required = false) Integer limit
  ) {
    // Refresh the user's coarse location for Smart Fill targeting.
    // Best-effort: never let it break the feed.
    try {
      userService.updateLastKnownLocation(user.getId(), latitude, longitude);
    } catch (Exception ignored) { /* feed must render regardless */ }

    return homeFeedService.build(user.getId(), latitude, longitude, radiusMeters, sportId, limit);
  }
}
