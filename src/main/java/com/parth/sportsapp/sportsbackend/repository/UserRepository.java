package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.User;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  // check if email exits
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<User> findByPhoneNumber(String phoneNumber); // finds phone number in DB

  boolean existsByPhoneNumber(String phoneNumber); // sees if phone number exists

  // In UserRepository.java
  Optional<User> findByVerificationToken(String token);

  boolean existsByUsername(String username);

  Optional<User> findByUsername(String username);

  Optional<User> findByGoogleId(String googleId);

  @Query("SELECT u FROM User u WHERE " +
      "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
      "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
      "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<User> searchUsers(@Param("query") String query);


}
