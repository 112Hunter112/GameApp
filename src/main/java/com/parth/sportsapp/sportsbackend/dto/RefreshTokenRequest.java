package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RefreshTokenRequest {

  @NotBlank(message = "refreshToken is required")
  @Size(max = 256, message = "refreshToken malformed")
  private String refreshToken;

  public RefreshTokenRequest() {}

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
