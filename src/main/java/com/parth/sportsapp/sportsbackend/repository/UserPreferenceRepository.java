package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Match;
import com.parth.sportsapp.sportsbackend.model.UserPreference;
import com.parth.sportsapp.sportsbackend.model.UserPreferenceId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UserPreferenceId> {

  //Finds preference for a specific user
  List<UserPreference> findByUserId(UUID userId);

  @Override
  Page<UserPreference> findAll(Pageable pageable);


  List<UserPreference> existsByUserId(UUID userId, Pageable pageable);


}
