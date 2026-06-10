package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.AwardImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AwardImageRepository extends JpaRepository<AwardImage, UUID> {

    long countByAwardId(UUID awardId);
}
