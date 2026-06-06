package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  @Size(max = 320, message = "Email too long")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(max = 128, message = "Password too long")
  @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
      message = "Password must be 8+ chars with 1 number, 1 uppercase, 1 lowercase, 1 special character (@#$%^&+=)")
  private String password;

  @NotBlank(message = "Password confirmation is required")
  @Size(max = 128, message = "Password confirmation too long")
  private String confirmPassword;

  @NotBlank(message = "Username is required")
  @Pattern(regexp = "^[a-z0-9_]{3,20}$",
      message = "Username must be 3-20 characters and can only contain lowercase letters, numbers, and underscores")
  private String username;

  @NotBlank(message = "First name is required")
  @Size(max = 50, message = "First name too long")
  private String firstName;

  @NotBlank(message = "Last name is required")
  @Size(max = 50, message = "Last name too long")
  private String lastName;

  @Size(max = 20)
  private String role;

  public RegisterRequest() {}

  public RegisterRequest(String email, String password, String confirmPassword,
      String firstName, String lastName) {
    this.email = email;
    this.password = password;
    this.confirmPassword = confirmPassword;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }

  public String getConfirmPassword() { return confirmPassword; }
  public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }

  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
}
