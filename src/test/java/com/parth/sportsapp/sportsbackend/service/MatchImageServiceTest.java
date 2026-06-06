package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.MatchImageRequest;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.Match;
import com.parth.sportsapp.sportsbackend.model.Participants;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.MatchImageRepository;
import com.parth.sportsapp.sportsbackend.repository.MatchRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Security-focused tests for {@link MatchImageService}. These exercise the
 * URL validation, the participant authorization check, the storage cap, and
 * the "only uploader can delete" rule.
 */
class MatchImageServiceTest {

  // --- URL validation -------------------------------------------------------

  @Test
  void rejectsNonHttpsSchemes() {
    MatchImageService svc = newService("");

    for (String bad : List.of(
        "http://imgur.com/abc.jpg",
        "javascript:alert(1)",
        "data:image/png;base64,AAAA",
        "file:///etc/passwd",
        "ftp://imgur.com/abc.jpg")) {
      assertThatThrownBy(() -> svc.addImage(UUID.randomUUID(), UUID.randomUUID(), req(bad)))
          .as("URL should be rejected: " + bad)
          .isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  void rejectsLoopbackAndPrivateHosts() {
    MatchImageService svc = newService("");

    for (String bad : List.of(
        "https://localhost/x.jpg",
        "https://127.0.0.1/x.jpg",
        "https://169.254.169.254/latest/meta-data/iam/security-credentials/", // AWS metadata
        "https://10.0.0.5/x.jpg",
        "https://192.168.1.1/x.jpg",
        "https://172.16.0.1/x.jpg",
        "https://172.31.255.255/x.jpg")) {
      assertThatThrownBy(() -> svc.addImage(UUID.randomUUID(), UUID.randomUUID(), req(bad)))
          .as("Private host should be rejected: " + bad)
          .isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  void rejectsHostNotOnAllowList() {
    MatchImageService svc = newService("cloudinary.com,amazonaws.com");

    assertThatThrownBy(() -> svc.addImage(UUID.randomUUID(), UUID.randomUUID(),
        req("https://random-attacker.example/x.jpg")))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("allow-list");
  }

  @Test
  void acceptsAllowListedHostAndSubdomain() {
    UUID matchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = newUser(userId);
    Match match = matchWith(matchId, user);

    MatchRepository matchRepo = mock(MatchRepository.class);
    MatchImageRepository imgRepo = mock(MatchImageRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    when(matchRepo.findById(matchId)).thenReturn(Optional.of(match));
    when(userRepo.findById(userId)).thenReturn(Optional.of(user));
    when(imgRepo.countByMatch_Id(matchId)).thenReturn(0L);
    when(imgRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    MatchImageService svc = new MatchImageService(imgRepo, matchRepo, userRepo,
        "cloudinary.com,imgur.com");

    // subdomain of allow-listed apex
    assertThatCode(() -> svc.addImage(matchId, userId,
        req("https://res.cloudinary.com/demo/image/upload/match.jpg")))
        .doesNotThrowAnyException();

    // exact match
    assertThatCode(() -> svc.addImage(matchId, userId,
        req("https://imgur.com/x.jpg"))).doesNotThrowAnyException();
  }

  @Test
  void allowsAnyNonPrivateHttpsHostWhenAllowListEmpty() {
    UUID matchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = newUser(userId);
    Match match = matchWith(matchId, user);

    MatchRepository matchRepo = mock(MatchRepository.class);
    MatchImageRepository imgRepo = mock(MatchImageRepository.class);
    UserRepository userRepo = mock(UserRepository.class);
    when(matchRepo.findById(matchId)).thenReturn(Optional.of(match));
    when(userRepo.findById(userId)).thenReturn(Optional.of(user));
    when(imgRepo.countByMatch_Id(matchId)).thenReturn(0L);
    when(imgRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    MatchImageService svc = new MatchImageService(imgRepo, matchRepo, userRepo, "");

    assertThatCode(() -> svc.addImage(matchId, userId,
        req("https://random-cdn.example.com/img.jpg"))).doesNotThrowAnyException();
  }

  // --- authorization & cap --------------------------------------------------

  @Test
  void rejectsNonParticipant() {
    UUID matchId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    UUID strangerId = UUID.randomUUID();
    Match match = matchWith(matchId, newUser(participantId));

    MatchRepository matchRepo = mock(MatchRepository.class);
    when(matchRepo.findById(matchId)).thenReturn(Optional.of(match));

    MatchImageService svc = new MatchImageService(
        mock(MatchImageRepository.class), matchRepo, mock(UserRepository.class), "");

    assertThatThrownBy(() -> svc.addImage(matchId, strangerId,
        req("https://imgur.com/x.jpg")))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void rejectsAddOverCap() {
    UUID matchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Match match = matchWith(matchId, newUser(userId));

    MatchRepository matchRepo = mock(MatchRepository.class);
    MatchImageRepository imgRepo = mock(MatchImageRepository.class);
    when(matchRepo.findById(matchId)).thenReturn(Optional.of(match));
    when(imgRepo.countByMatch_Id(matchId))
        .thenReturn((long) MatchImageService.MAX_IMAGES_PER_MATCH);

    MatchImageService svc = new MatchImageService(
        imgRepo, matchRepo, mock(UserRepository.class), "");

    assertThatThrownBy(() -> svc.addImage(matchId, userId,
        req("https://imgur.com/x.jpg")))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("maximum");
  }

  @Test
  void deleteRequiresUploader() {
    UUID imageId = UUID.randomUUID();
    UUID uploaderId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();

    com.parth.sportsapp.sportsbackend.model.MatchImage img =
        new com.parth.sportsapp.sportsbackend.model.MatchImage();
    img.setId(imageId);
    img.setUploadedBy(newUser(uploaderId));

    MatchImageRepository imgRepo = mock(MatchImageRepository.class);
    when(imgRepo.findById(imageId)).thenReturn(Optional.of(img));

    MatchImageService svc = new MatchImageService(
        imgRepo, mock(MatchRepository.class), mock(UserRepository.class), "");

    assertThatThrownBy(() -> svc.deleteImage(imageId, otherId))
        .isInstanceOf(ForbiddenException.class);

    // The uploader can delete
    assertThatCode(() -> svc.deleteImage(imageId, uploaderId)).doesNotThrowAnyException();
    verify(imgRepo).delete(img);
  }

  @Test
  void addOnMissingMatchReturns404() {
    MatchRepository matchRepo = mock(MatchRepository.class);
    when(matchRepo.findById(any())).thenReturn(Optional.empty());

    MatchImageService svc = new MatchImageService(
        mock(MatchImageRepository.class), matchRepo, mock(UserRepository.class), "");

    assertThatThrownBy(() -> svc.addImage(UUID.randomUUID(), UUID.randomUUID(),
        req("https://imgur.com/x.jpg"))).isInstanceOf(NotFoundException.class);
  }

  // --- helpers --------------------------------------------------------------

  private static MatchImageRequest req(String url) {
    MatchImageRequest r = new MatchImageRequest();
    r.setImageUrl(url);
    return r;
  }

  private static User newUser(UUID id) {
    User u = new User();
    u.setId(id);
    u.setFirstName("First");
    u.setLastName("Last");
    return u;
  }

  private static Match matchWith(UUID matchId, User participant) {
    Match m = new Match();
    m.setId(matchId);
    Participants p = new Participants();
    p.setUser(participant);
    p.setTeamName("TEAM_A");
    m.setParticipants(new java.util.ArrayList<>(List.of(p)));
    return m;
  }

  /** Build a service with only the URL guards exercised (other reads stubbed minimally). */
  private static MatchImageService newService(String allowedHosts) {
    return new MatchImageService(
        mock(MatchImageRepository.class),
        mock(MatchRepository.class),
        mock(UserRepository.class),
        allowedHosts);
  }
}
