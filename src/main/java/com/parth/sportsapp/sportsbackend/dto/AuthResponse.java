package com.parth.sportsapp.sportsbackend.dto;

public class AuthResponse {

  private String token;
  private String email;
  private String firstName;
  private String role;

  // No-arg constructor (required for JSON serialization)
  public AuthResponse() {
  }

  // All-args constructor (convenient for creating response in service)
  public AuthResponse(String token, String email, String firstName, String role) {
    this.token = token;
    this.email = email;
    this.firstName = firstName;
    this.role = role;
  }

  // Getters (Spring needs these to convert an object → JSON)
  public String getToken() {
    return token;
  }

  public String getEmail() {
    return email;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getRole() {
    return role;
  }

  // Setters (Spring needs these for flexibility, though less used in responses)
  public void setToken(String token) {
    this.token = token;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
