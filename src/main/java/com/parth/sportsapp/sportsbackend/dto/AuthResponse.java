package com.parth.sportsapp.sportsbackend.dto;

public class AuthResponse {

  private String token;          // short-lived access token (JWT)
  private String refreshToken;   // long-lived refresh token (opaque, single-use)
  private Long expiresIn;        // access-token lifetime in seconds
  private String email;
  private String firstName;
  private String role;

  public AuthResponse() {
  }

  // Backward-compatible 4-arg constructor (access token only).
  public AuthResponse(String token, String email, String firstName, String role) {
    this.token = token;
    this.email = email;
    this.firstName = firstName;
    this.role = role;
  }

  // Full constructor with refresh token + expiry.
  public AuthResponse(String token, String refreshToken, Long expiresIn,
                      String email, String firstName, String role) {
    this.token = token;
    this.refreshToken = refreshToken;
    this.expiresIn = expiresIn;
    this.email = email;
    this.firstName = firstName;
    this.role = role;
  }

  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }

  public String getRefreshToken() { return refreshToken; }
  public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

  public Long getExpiresIn() { return expiresIn; }
  public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
}
