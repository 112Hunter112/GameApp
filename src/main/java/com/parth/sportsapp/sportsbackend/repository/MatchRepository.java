package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Match;
import com.parth.sportsapp.sportsbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

  //Match results in string format (can format them in the service layer for the specific sport)

  //which user created this match or which user made this manual match history?

  // source of the Booking such as APP_BOOKING, MANUAL_ENTRY, EXTERNAL(For API)

  // verification status of the match listed

  //find total matches won by the user
  List<Match> findByWinner(User winner);

  // number of wins by the User
  long countByWinner(User winner);

  //matches made by user
  List<Match> findByCreatedByUser(User user);
}
