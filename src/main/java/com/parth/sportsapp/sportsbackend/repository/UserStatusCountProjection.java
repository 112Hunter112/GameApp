package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.BookingStatus;

import java.util.UUID;

/** One (user, status, count) row from the reliability aggregate. */
public interface UserStatusCountProjection {
  UUID getUserId();
  BookingStatus getStatus();
  Long getCnt();
}
