package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.UserSummaryDto;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

  @Autowired
  private UserRepository userRepository;

  public List<UserSummaryDto> searchUsers(String query) {
    List<User> users = userRepository.searchUsers(query);

    // Convert to DTOs immediately to protect privacy (passwords/phones)
    return users.stream()
        .map(user -> new UserSummaryDto(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail()
        ))
        .collect(Collectors.toList());
  }
}
