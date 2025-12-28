package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.AuthResponse;
import com.parth.sportsapp.sportsbackend.dto.LoginRequest;
import com.parth.sportsapp.sportsbackend.dto.RegisterRequest;
import com.parth.sportsapp.sportsbackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.HttpsURLConnection;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")  // Explicit, but still allows all // this allows requesats from
// react native app

/**
 * down the line wew can use @CrossOrigin(origins = "http://localhost:19006")  // React Native dev server
 * // OR configure globally in SecurityConfig
 */
public class AuthController {

  @Autowired
  private AuthService authService;



  /**
   * // we use the dto as the paramter here, but it is made into a java object using RequestBody
   *     // which makes from JSON to java object as requested
   *     //register the user, this will insert the dat into the table
   *   // we use the dto as the paramter here, but it is made into a java object using RequestBody
   *   // which makes from JSON to java object as requested
   *This will be accessible at: POST http://localhost:8080/auth/register
   * @param registerRequest holds data of user from frontend
   * @return
   */
  @PostMapping("/register") // put data on the database
  public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {

    try {
      //authService takes the input from frontned as registerRequest and makes it into output as AuthResponse
      String authResponse = authService.register(registerRequest); //

      return ResponseEntity.ok(authResponse);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }

  }

  // This will be accessible at: POST http://localhost:8080/auth/login
  //? tells the user that th personmigth return a random variable String(for error) or AuthResponse
  @PostMapping("/login")
  public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {

    try {
      AuthResponse authResponse = authService.login(loginRequest);
      return ResponseEntity.ok(authResponse);
    } catch (RuntimeException e) {
      // Now "User not found" or "Not Verified" returns 400 Bad Request, not 500 Crash
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }


  @GetMapping("/verify")
  public ResponseEntity<String> verifyUser(@RequestParam("token") String token) {
    try {
      String result = authService.verifyAccount(token);
      return ResponseEntity.ok(result);
    } catch (RuntimeException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
