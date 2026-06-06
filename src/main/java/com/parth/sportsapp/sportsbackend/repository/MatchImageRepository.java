package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.MatchImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchImageRepository extends JpaRepository<MatchImage, UUID> {

  /** Images for one match, newest first. */
  List<MatchImage> findByMatch_IdOrderByCreatedAtDesc(UUID matchId);

  long countByMatch_Id(UUID matchId);
}
