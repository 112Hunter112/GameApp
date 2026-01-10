package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Match;
import com.parth.sportsapp.sportsbackend.model.Participants;
import com.parth.sportsapp.sportsbackend.model.ParticipantsId;
import com.parth.sportsapp.sportsbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantsRepository extends JpaRepository<Participants, ParticipantsId> {

  // 1. Find all players in a specific match
  List<Participants> findByMatch(Match match);

  // 2. Find all matches a specific user is in
  List<Participants> findByUser(User user);

  // 3. Find a specific participant entry (Alternative to findById)
  // Useful if you have the User and Match objects but not the composite ID object
  Participants findByMatchAndUser(Match match, User user);
}
