package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.model.PasswordResetCode;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.PasswordResetCodeRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  private static final String VAGUE_ERROR =
      "Invalid or expired code. Request a new one and try again.";

  @Mock private UserRepository userRepository;
  @Mock private PasswordResetCodeRepository codeRepository;
  @Mock private EmailService emailService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private PwnedPasswordService pwnedPasswordService;
  @Mock private RefreshTokenService refreshTokenService;

  @InjectMocks private PasswordResetService service;

  // --- requestCode -----------------------------------------------------------

  @Test
  void requestCodeForUnknownEmailStaysSilent() {
    // No user enumeration: unknown email must behave exactly like success —
    // no exception, no code stored, no email sent.
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

    service.requestCode("ghost@example.com");

    verifyNoInteractions(codeRepository, emailService);
  }

  @Test
  void requestCodeNormalizesEmailBeforeLookup() {
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

    service.requestCode("  USER@Example.COM ");

    verify(userRepository).findByEmail("user@example.com");
  }

  @Test
  void requestCodeStoresOnlyTheHashAndEmailsThePlaintext() {
    User user = user();
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(codeRepository.countByUserIdAndCreatedAtAfter(eq(user.getId()), any(LocalDateTime.class)))
        .thenReturn(0L);

    service.requestCode(user.getEmail());

    // Previous codes are invalidated so only one code is live at a time.
    verify(codeRepository).invalidateAllForUser(user.getId());

    ArgumentCaptor<PasswordResetCode> savedCaptor = ArgumentCaptor.forClass(PasswordResetCode.class);
    verify(codeRepository).save(savedCaptor.capture());
    PasswordResetCode saved = savedCaptor.getValue();

    ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendPasswordResetCode(eq(user.getEmail()), eq(user.getFirstName()),
        codeCaptor.capture());
    String emailedCode = codeCaptor.getValue();

    // The emailed code is 6 digits; the database only ever sees its SHA-256.
    assertThat(emailedCode).matches("\\d{6}");
    assertThat(saved.getCodeHash()).isEqualTo(sha256Hex(emailedCode));
    assertThat(saved.getCodeHash()).isNotEqualTo(emailedCode);
    assertThat(saved.getUserId()).isEqualTo(user.getId());
    assertThat(saved.getExpiresAt())
        .isAfter(LocalDateTime.now())
        .isBefore(LocalDateTime.now().plusMinutes(PasswordResetCode.EXPIRY_MINUTES + 1));
  }

  @Test
  void requestCodeIsRateLimitedToThreePerHour() {
    User user = user();
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(codeRepository.countByUserIdAndCreatedAtAfter(eq(user.getId()), any(LocalDateTime.class)))
        .thenReturn(3L);

    service.requestCode(user.getEmail());

    // Silently dropped: no new code, no invalidation, no email — and no error
    // signal the caller could use to probe the limit.
    verify(codeRepository, never()).invalidateAllForUser(any());
    verify(codeRepository, never()).save(any());
    verifyNoInteractions(emailService);
  }

  // --- resetPassword ---------------------------------------------------------

  @Test
  void resetPasswordWithCorrectCodeUpdatesPasswordAndKillsAllSessions() {
    User user = user();
    PasswordResetCode stored = storedCode(user.getId(), "123456");
    stubActiveCode(user, stored);
    when(pwnedPasswordService.isBreached("NewPassw0rd")).thenReturn(false);
    when(passwordEncoder.encode("NewPassw0rd")).thenReturn("ENCODED");

    service.resetPassword(user.getEmail(), "123456", "NewPassw0rd");

    assertThat(stored.isUsed()).isTrue();
    verify(codeRepository).save(stored);
    assertThat(user.getPassword()).isEqualTo("ENCODED");
    verify(userRepository).save(user);
    // A reset triggered by a hijack must end the hijacker's sessions too.
    verify(refreshTokenService).revokeAllForUser(user.getId());
  }

  @Test
  void resetPasswordTrimsTheSubmittedCode() {
    User user = user();
    PasswordResetCode stored = storedCode(user.getId(), "123456");
    stubActiveCode(user, stored);
    when(pwnedPasswordService.isBreached("NewPassw0rd")).thenReturn(false);
    when(passwordEncoder.encode("NewPassw0rd")).thenReturn("ENCODED");

    service.resetPassword(user.getEmail(), " 123456 ", "NewPassw0rd");

    assertThat(stored.isUsed()).isTrue();
  }

  @Test
  void resetPasswordWithWrongCodeCountsTheAttempt() {
    User user = user();
    PasswordResetCode stored = storedCode(user.getId(), "123456");
    stubActiveCode(user, stored);

    assertThatThrownBy(() -> service.resetPassword(user.getEmail(), "654321", "NewPassw0rd"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(VAGUE_ERROR);

    assertThat(stored.getAttempts()).isEqualTo(1);
    verify(codeRepository).save(stored);
    verify(userRepository, never()).save(any());
    verifyNoInteractions(refreshTokenService);
  }

  @Test
  void resetPasswordRejectsExpiredCode() {
    User user = user();
    PasswordResetCode stored = storedCode(user.getId(), "123456");
    stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    stubActiveCode(user, stored);

    assertThatThrownBy(() -> service.resetPassword(user.getEmail(), "123456", "NewPassw0rd"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(VAGUE_ERROR);

    verify(codeRepository, never()).save(any());
  }

  @Test
  void resetPasswordRejectsCodeAfterMaxAttempts() {
    User user = user();
    PasswordResetCode stored = storedCode(user.getId(), "123456");
    stored.setAttempts(PasswordResetCode.MAX_ATTEMPTS);
    stubActiveCode(user, stored);

    // Even the RIGHT code is rejected once the code is locked.
    assertThatThrownBy(() -> service.resetPassword(user.getEmail(), "123456", "NewPassw0rd"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(VAGUE_ERROR);
  }

  @Test
  void resetPasswordFailuresAreIndistinguishable() {
    // Unknown email and missing code must throw the SAME vague error, so a
    // caller cannot tell which part was wrong.
    when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.resetPassword("ghost@example.com", "123456", "NewPassw0rd"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(VAGUE_ERROR);

    User user = user();
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(codeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId()))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.resetPassword(user.getEmail(), "123456", "NewPassw0rd"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage(VAGUE_ERROR);
  }

  @Test
  void resetPasswordRejectsWeakPasswordsButKeepsCodeUsable() {
    User user = user();
    PasswordResetCode stored = storedCode(user.getId(), "123456");
    stubActiveCode(user, stored);

    // Too short.
    assertThatThrownBy(() -> service.resetPassword(user.getEmail(), "123456", "abc1"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("at least 8 characters");

    // No letter.
    assertThatThrownBy(() -> service.resetPassword(user.getEmail(), "123456", "12345678"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("letter and one number");

    // The code was correct, so the user may retry with a better password:
    // it must not be consumed or penalized.
    assertThat(stored.isUsed()).isFalse();
    assertThat(stored.getAttempts()).isZero();
    verify(codeRepository, never()).save(any());
  }

  @Test
  void resetPasswordRejectsBreachedPassword() {
    User user = user();
    PasswordResetCode stored = storedCode(user.getId(), "123456");
    stubActiveCode(user, stored);
    when(pwnedPasswordService.isBreached("Password123")).thenReturn(true);

    assertThatThrownBy(() -> service.resetPassword(user.getEmail(), "123456", "Password123"))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("data breach");

    verify(userRepository, never()).save(any());
  }

  // --- helpers ---------------------------------------------------------------

  private void stubActiveCode(User user, PasswordResetCode stored) {
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(codeRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId()))
        .thenReturn(Optional.of(stored));
  }

  private User user() {
    User u = new User();
    u.setId(UUID.randomUUID());
    u.setEmail("user@example.com");
    u.setFirstName("Parth");
    u.setLastName("A");
    u.setPassword("old-hash");
    return u;
  }

  private PasswordResetCode storedCode(UUID userId, String plaintext) {
    PasswordResetCode c = new PasswordResetCode();
    c.setId(UUID.randomUUID());
    c.setUserId(userId);
    c.setCodeHash(sha256Hex(plaintext));
    c.setExpiresAt(LocalDateTime.now().plusMinutes(PasswordResetCode.EXPIRY_MINUTES));
    c.setAttempts(0);
    c.setUsed(false);
    return c;
  }

  private static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
