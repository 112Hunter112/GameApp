package com.parth.sportsapp.sportsbackend.dto;

public class StreakDto {
  private String type; // "WIN", "LOSS", "NONE"
  private int count;

  public StreakDto(String type, int count) {
    this.type = type;
    this.count = count;
  }

  // Getters and setters

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }
}
