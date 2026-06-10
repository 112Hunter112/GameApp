package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.AuthResponse;
import com.parth.sportsapp.sportsbackend.dto.LoginRequest;
import com.parth.sportsapp.sportsbackend.dto.RegisterRequest;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserRole;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private EmailService emailService;

  @Autowired
  private PwnedPasswordService pwnedPasswordService;

  @Autowired
  private RefreshTokenService refreshTokenService;

  /**
   * This method outputs the AuthResponse if all test cases pass and also adds the data to the
   * DB by making it an object.
   *
   *
   * todo : must change method type to String
   * @param registerRequest
   * @return
   */
  @Transactional
  public String register(RegisterRequest registerRequest) {

    if(userRepository.existsByEmail(registerRequest.getEmail())) {
      throw new RuntimeException("Email already in use");
    }

    if(userRepository.existsByUsername(registerRequest.getUsername().toLowerCase())) {
      throw new RuntimeException("Username already taken");
    }

    if(!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
      throw new RuntimeException("Passwords do not match");
    }

    // NIST SP 800-63B: reject passwords that appear in known breach corpora.
    if (pwnedPasswordService.isBreached(registerRequest.getPassword())) {
      throw new RuntimeException(
          "This password has appeared in a data breach. Please choose a different one.");
    }

    User newUser = new User();
    newUser.setEmail(registerRequest.getEmail());
    newUser.setUsername(registerRequest.getUsername().toLowerCase());
    newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    newUser.setFirstName(registerRequest.getFirstName());
    newUser.setLastName(registerRequest.getLastName());
    newUser.setVerified(true);

    if (registerRequest.getRole() != null && registerRequest.getRole().equalsIgnoreCase("VENUE_OWNER")) {
      newUser.setRole(UserRole.VENUE_OWNER);
    } else {
      newUser.setRole(UserRole.USER);
    }

    userRepository.save(newUser);

    return "Account created successfully";
  }

  // A throwaway BCrypt hash, computed once, used to equalize login response time
  // when the email does not exist (timing-attack / user-enumeration defense).
  private volatile String dummyHash;

  private String dummyHash() {
    String local = dummyHash;
    if (local == null) {
      synchronized (this) {
        local = dummyHash;
        if (local == null) {
          // Encoded at the same cost factor as real passwords, so a comparison
          // against it takes the same wall-clock time as a real comparison.
          local = passwordEncoder.encode("timing-safe-placeholder-password");
          dummyHash = local;
        }
      }
    }
    return local;
  }

  public AuthResponse login(LoginRequest request) {

    // Step 1: Find the user by email.
    // SECURITY: use the SAME error for "no such user" and "wrong password" so an
    // attacker cannot enumerate which emails are registered (ASVS V2.2 / V3).
    User user = userRepository.findByEmail(request.getEmail())
        .orElse(null);

    boolean isMatch;
    if (user != null) {
      isMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
    } else {
      // No such user: still run a BCrypt comparison against a dummy hash so the
      // response takes the same time as a real (failed) login. Without this, a
      // missing email returns in ~1ms while a real email takes ~250ms, letting an
      // attacker enumerate accounts by timing despite the identical error message.
      passwordEncoder.matches(request.getPassword(), dummyHash());
      isMatch = false;
    }

    if (!isMatch) {
      throw new RuntimeException("Invalid email or password");
    }

    if (!Boolean.TRUE.equals(user.getVerified())) {
      throw new RuntimeException("Account not verified. Please check your email.");
    }

    // Step 3: Generate a short-lived access token + a long-lived refresh token.
    String accessToken = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().toString());
    String refreshToken = refreshTokenService.issue(user.getId());

    // Step 4: Return both.
    return new AuthResponse(
        accessToken,
        refreshToken,
        jwtUtil.getAccessTokenExpiryMs() / 1000,
        user.getEmail(),
        user.getFirstName(),
        user.getRole().name()
    );
  }


  public String verifyAccount(String token) {

    // Find the user by the token
    User user = userRepository.findByVerificationToken(token)
        .orElseThrow(() -> new RuntimeException("Invalid verification token"));


    // We compare "Now" against the "Expiry Time" saved in the DB.
    if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
      throw new RuntimeException("Token has expired. Please register again.");
    }

    // Check if already verified to save DB calls
    if (Boolean.TRUE.equals(user.getVerified())) {
      return "Account is already verified.";
    }

    // 4. Activate the user
    user.setVerified(true);


    // Remove the token so it can't be used again
    user.setVerificationToken(null);
    user.setVerificationTokenExpiry(null);

    // 6. Save changes
    userRepository.save(user);

    return "Account verified successfully!";
  }


}
