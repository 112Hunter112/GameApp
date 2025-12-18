package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.AuthResponse;
import com.parth.sportsapp.sportsbackend.dto.LoginRequest;
import com.parth.sportsapp.sportsbackend.dto.RegisterRequest;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserRole;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  public AuthResponse register(RegisterRequest registerRequest) {
    // user RegisterRequest DTO to take info from frontend, use RegisterRequest as param to take


    //process the input and check with userRepository to check if it exists

   // if both email and number are correct, and then passwrd is the same
   if(userRepository.existsByEmail(registerRequest.getEmail())) {
     throw new RuntimeException("Email already in use");
   }

   if(userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
     throw new RuntimeException("Phone number already in use");
   }
    // if email/number does not exist in DB, then pass through password encoder
   if(!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
     // if the password is wrong, return null
     throw new RuntimeException("Password does not match");
   }
    // todo : then push the info into db
    User newUser = new User();
    newUser.setEmail(registerRequest.getEmail());
    newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    newUser.setFirstName(registerRequest.getFirstName());
    newUser.setLastName(registerRequest.getLastName());
    newUser.setPhoneNumber(registerRequest.getPhoneNumber());
    newUser.setRole(UserRole.USER); // Set the default role

    User savedUser = userRepository.save(newUser);

    String token = jwtUtil.generateToken(savedUser.getEmail());


    //todo : then we generate jwt token with jwtUtil and send to AuthResponce to frontend


    return new AuthResponse(
        token,
        savedUser.getEmail(),
        savedUser.getFirstName(),
        savedUser.getRole().name() // .name() converts the Enum to a String
    );

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


}
