package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email")
  private String email;

  @NotBlank(message = "Code is required")
  @Pattern(regexp = "^[0-9]{6}$", message = "Code must be 6 digits")
  private String code;

  @NotBlank(message = "New password is required")
  @Size(min = 8, max = 128, message = "Password must be 8-128 characters")
  private String newPassword;

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }

  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }

  public String getNewPassword() { return newPassword; }
  public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
