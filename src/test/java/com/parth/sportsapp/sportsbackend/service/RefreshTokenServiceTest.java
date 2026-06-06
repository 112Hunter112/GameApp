package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.AuthResponse;
import com.parth.sportsapp.sportsbackend.exception.UnauthorizedException;
import com.parth.sportsapp.sportsbackend.model.RefreshToken;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserRole;
import com.parth.sportsapp.sportsbackend.repository.RefreshTokenRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private JwtUtil jwtUtil;

  private RefreshTokenService service;

  private static final long REFRESH_MS = 2_592_000_000L; // 30 days

  @BeforeEach
  void setUp() {
    service = new RefreshTokenService(refreshTokenRepository, userRepository, jwtUtil, REFRESH_MS);
  }

  @Test
  void issueReturnsPlaintextAndStoresOnlyTheHash() {
    String plaintext = service.issue(UUID.randomUUID());

    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository).save(captor.capture());
    RefreshToken saved = captor.getValue();

    // The stored value must NOT equal the plaintext returned to the client.
    assertThat(saved.getTokenHash()).isNotEqualTo(plaintext);
    assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex
    assertThat(saved.isRevoked()).isFalse();
    assertThat(saved.getExpiryDate()).isAfter(Instant.now());
    assertThat(plaintext).isNotBlank();
  }

  @Test
  void refreshRotatesTokenAndReturnsNewPair() {
    UUID userId = UUID.randomUUID();
    RefreshToken existing = validStored(userId);
    User user = user(userId);

    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(jwtUtil.generateToken(anyString(), any(UUID.class), anyString())).thenReturn("new-access");
    when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(900_000L);

    AuthResponse res = service.refresh("some-plaintext-token");

    assertThat(res.getToken()).isEqualTo("new-access");
    assertThat(res.getRefreshToken()).isNotBlank();
    assertThat(res.getExpiresIn()).isEqualTo(900L);
    // Old token must be revoked, and a new one saved (rotation = 2 saves total).
    assertThat(existing.isRevoked()).isTrue();
    verify(refreshTokenRepository, atLeast(2)).save(any(RefreshToken.class));
  }

  @Test
  void refreshDetectsReuseOfRevokedTokenAndKillsAllSessions() {
    UUID userId = UUID.randomUUID();
    RefreshToken revoked = validStored(userId);
    revoked.setRevoked(true);

    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

    assertThatThrownBy(() -> service.refresh("stolen-token"))
        .isInstanceOf(UnauthorizedException.class);

    verify(refreshTokenRepository).revokeAllForUser(userId);
  }

  @Test
  void refreshRejectsExpiredToken() {
    UUID userId = UUID.randomUUID();
    RefreshToken expired = validStored(userId);
    expired.setExpiryDate(Instant.now().minusSeconds(60));

    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> service.refresh("expired-token"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void refreshRejectsUnknownToken() {
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.refresh("nonexistent"))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void refreshRejectsNullOrBlank() {
    assertThatThrownBy(() -> service.refresh(null)).isInstanceOf(UnauthorizedException.class);
    assertThatThrownBy(() -> service.refresh("  ")).isInstanceOf(UnauthorizedException.class);
    verifyNoInteractions(userRepository);
  }

  @Test
  void revokeMarksTokenRevoked() {
    RefreshToken token = validStored(UUID.randomUUID());
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    service.revoke("some-token");

    assertThat(token.isRevoked()).isTrue();
    verify(refreshTokenRepository).save(token);
  }

  // --- helpers -------------------------------------------------------------

  private RefreshToken validStored(UUID userId) {
    RefreshToken t = new RefreshToken();
    t.setId(UUID.randomUUID());
    t.setUserId(userId);
    t.setTokenHash("hash");
    t.setExpiryDate(Instant.now().plusSeconds(3600));
    t.setRevoked(false);
    t.setCreatedAt(Instant.now());
    return t;
  }

  private User user(UUID id) {
    User u = new User();
    u.setId(id);
    u.setEmail("a@b.com");
    u.setFirstName("First");
    u.setRole(UserRole.USER);
    return u;
  }
}
