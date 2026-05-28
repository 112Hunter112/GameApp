package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleLoginRequest {

  @NotBlank(message = "idToken is required")
  private String idToken;

  public GoogleLoginRequest() {}

  public String getIdToken() {
    return idToken;
  }

  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }
}
