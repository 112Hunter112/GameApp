package com.parth.sportsapp.sportsbackend.dto;

public class StreakDto {
  private String type; // "WIN", "LOSS", "NONE"
  private int count;

  public StreakDto(String type, int count) {
    this.type = type;
    this.count = count;
  }

  // Getters and setters
}
