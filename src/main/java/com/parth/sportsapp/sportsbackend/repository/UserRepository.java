package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.User;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  // check if email exits
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<User> findByPhoneNumber(String phoneNumber); // finds phone number in DB

  boolean existsByPhoneNumber(String phoneNumber); // sees if phone number exists

  //User save(User user); this is an inbuilt function of JpaRepository
}
