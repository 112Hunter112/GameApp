package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.BookingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookingPolicyRepository extends JpaRepository<BookingPolicy, UUID> {

  Optional<BookingPolicy> findByVenue_Id(UUID venueId);
}
