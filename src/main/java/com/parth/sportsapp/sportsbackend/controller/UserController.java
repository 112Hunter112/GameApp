package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

  @Autowired
  private UserService userService;

  @GetMapping("/search")
  public ResponseEntity<List<UserSummaryDto>> searchUsers(@RequestParam("query") String query) {
    if (query == null || query.trim().length() < 2) {
      return ResponseEntity.ok(List.of()); // Return empty if query is too short
    }
    return ResponseEntity.ok(userService.searchUsers(query));
  }
}
