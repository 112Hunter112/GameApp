package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.AuthResponse;
import com.parth.sportsapp.sportsbackend.dto.LoginRequest;
import com.parth.sportsapp.sportsbackend.dto.RegisterRequest;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserRole;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import jakarta.transaction.Transactional;
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
    // user RegisterRequest DTO to take info from frontend, use RegisterRequest as param to take


    //process the input and check with userRepository to check if it exists

   // if both email and number are correct, and then passwrd is the same
   if(userRepository.existsByEmail(registerRequest.getEmail())) {
     throw new RuntimeException("Email already in use");
   }

   if(userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
     throw new RuntimeException("Phone number already in use");
   }


    // if email and number do not exist in DB, then pass through password encoder
   if(!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
     // if the password is wrong, return null
     throw new RuntimeException("Password does not match");
   }

    User newUser = new User();
    newUser.setEmail(registerRequest.getEmail());
    newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    newUser.setFirstName(registerRequest.getFirstName());
    newUser.setLastName(registerRequest.getLastName());
    newUser.setPhoneNumber(registerRequest.getPhoneNumber());
    newUser.setRole(UserRole.USER); // Set the default role


    // verify email

    newUser.setVerified(false); // set initial verified as false so that user gets veified via email

    String vToken = UUID.randomUUID().toString();

    newUser.setVerificationToken(vToken);
    newUser.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(30));


    User savedUser = userRepository.save(newUser);


    String link = "http://localhost:8080/api/auth/verify?token=" + vToken;

    emailService.sendMailWithAttachment(registerRequest.getEmail(),
        "verification mail for DuoSprt", link);


    return "Verification email sent";
  }

  public AuthResponse login(LoginRequest request) {

    // Step 1: Find the user by email
    // .orElseThrow() is a cleaner way to handle "User not found" than using "if (user == null)"
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new RuntimeException("User not found"));

    // Step 2: Validate the password
    // We compare the Raw password (request) vs the Encrypted password (database)
    boolean isMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());

    if (!isMatch) {
      throw new RuntimeException("Invalid credentials");
    }

    if (!Boolean.TRUE.equals(user.getVerified())) {
      throw new RuntimeException("Account not verified. Please check your email.");
    }

    // Step 3: Generate the Token
    String token = jwtUtil.generateToken(user.getEmail());

    // Step 4: Return the response
    return new AuthResponse(
        token,
        user.getEmail(),
        user.getFirstName(),
        user.getRole().name() // Converts Enum (USER) to String ("USER")
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
