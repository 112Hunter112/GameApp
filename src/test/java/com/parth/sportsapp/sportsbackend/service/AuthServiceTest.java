package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.AuthResponse;
import com.parth.sportsapp.sportsbackend.dto.LoginRequest;
import com.parth.sportsapp.sportsbackend.dto.RegisterRequest;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserRole;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private JwtUtil jwtUtil;
  @Mock private UserRepository userRepository;
  @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
  @Mock private EmailService emailService;
  @Mock private PwnedPasswordService pwnedPasswordService;
  @Mock private RefreshTokenService refreshTokenService;

  @InjectMocks private AuthService authService;

  // --- registration --------------------------------------------------------

  @Test
  void registerSucceedsForNewUser() {
    RegisterRequest req = registerReq("a@b.com", "Str0ng!Pass", "Str0ng!Pass", "parth");
    when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
    when(userRepository.existsByUsername("parth")).thenReturn(false);
    when(pwnedPasswordService.isBreached(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hash");

    String result = authService.register(req);

    assertThat(result).isEqualTo("Account created successfully");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void registerRejectsDuplicateEmail() {
    RegisterRequest req = registerReq("a@b.com", "Str0ng!Pass", "Str0ng!Pass", "parth");
    when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(req))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Email already in use");
    verify(userRepository, never()).save(any());
  }

  @Test
  void registerRejectsMismatchedPasswords() {
    RegisterRequest req = registerReq("a@b.com", "Str0ng!Pass", "Different!1", "parth");
    when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
    when(userRepository.existsByUsername("parth")).thenReturn(false);

    assertThatThrownBy(() -> authService.register(req))
        .hasMessageContaining("Passwords do not match");
    verify(userRepository, never()).save(any());
  }

  @Test
  void registerRejectsBreachedPassword() {
    RegisterRequest req = registerReq("a@b.com", "Password1!", "Password1!", "parth");
    when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
    when(userRepository.existsByUsername("parth")).thenReturn(false);
    when(pwnedPasswordService.isBreached("Password1!")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(req))
        .hasMessageContaining("data breach");
    verify(userRepository, never()).save(any());
  }

  // --- login ---------------------------------------------------------------

  @Test
  void loginSucceedsAndReturnsAccessAndRefreshTokens() {
    User user = verifiedUser("a@b.com");
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Str0ng!Pass", user.getPassword())).thenReturn(true);
    when(jwtUtil.generateToken(eq("a@b.com"), any(UUID.class), anyString())).thenReturn("access-jwt");
    when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(900_000L);
    when(refreshTokenService.issue(user.getId())).thenReturn("refresh-token");

    AuthResponse res = authService.login(new LoginRequest("a@b.com", "Str0ng!Pass"));

    assertThat(res.getToken()).isEqualTo("access-jwt");
    assertThat(res.getRefreshToken()).isEqualTo("refresh-token");
    assertThat(res.getExpiresIn()).isEqualTo(900L);
    assertThat(res.getEmail()).isEqualTo("a@b.com");
  }

  @Test
  void loginWithWrongPasswordThrowsGenericError() {
    User user = verifiedUser("a@b.com");
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrongpass", user.getPassword())).thenReturn(false);

    assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrongpass")))
        .hasMessageContaining("Invalid email or password");
  }

  @Test
  void loginWithUnknownEmailRunsDummyComparisonAndThrowsGenericError() {
    when(userRepository.findByEmail("ghost@b.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$dummy");

    assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@b.com", "whatever1")))
        .hasMessageContaining("Invalid email or password");

    // Timing-safe: a BCrypt comparison must still run even though no user exists.
    verify(passwordEncoder).matches(eq("whatever1"), anyString());
  }

  @Test
  void loginRejectsUnverifiedAccount() {
    User user = verifiedUser("a@b.com");
    user.setVerified(false);
    when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Str0ng!Pass", user.getPassword())).thenReturn(true);

    assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "Str0ng!Pass")))
        .hasMessageContaining("not verified");
    verify(refreshTokenService, never()).issue(any());
  }

  // --- helpers -------------------------------------------------------------

  private RegisterRequest registerReq(String email, String pw, String confirm, String username) {
    RegisterRequest r = new RegisterRequest();
    r.setEmail(email);
    r.setPassword(pw);
    r.setConfirmPassword(confirm);
    r.setUsername(username);
    r.setFirstName("First");
    r.setLastName("Last");
    return r;
  }

  private User verifiedUser(String email) {
    User u = new User();
    u.setId(UUID.randomUUID());
    u.setEmail(email);
    u.setFirstName("First");
    u.setPassword("$2a$12$storedhash");
    u.setRole(UserRole.USER);
    u.setVerified(true);
    return u;
  }
}
