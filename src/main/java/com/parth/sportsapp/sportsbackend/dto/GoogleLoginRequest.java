package com.parth.sportsapp.sportsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GoogleLoginRequest {

  @NotBlank(message = "idToken is required")
  @Size(max = 4096, message = "idToken too long")
  private String idToken;

  public GoogleLoginRequest() {}

  public String getIdToken() {
    return idToken;
  }

  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }
}
