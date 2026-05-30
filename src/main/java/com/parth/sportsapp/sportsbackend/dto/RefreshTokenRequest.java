package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {

  @NotBlank(message = "refreshToken is required")
  private String refreshToken;

  public RefreshTokenRequest() {}

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
