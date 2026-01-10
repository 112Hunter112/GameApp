package com.parth.sportsapp.sportsbackend.model;

public enum ScoringType {
  SETS,   // Tennis, Volleyball (e.g., "6-4, 6-3")
  POINTS, // Soccer, Basketball (e.g., "2-1")
  TIME,   // Running, Swimming (e.g., "00:30:00")
  NONE    // Simple Win/Loss
}
