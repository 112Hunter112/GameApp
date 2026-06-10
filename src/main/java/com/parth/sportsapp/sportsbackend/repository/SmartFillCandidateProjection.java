package com.parth.sportsapp.sportsbackend.repository;

import java.util.UUID;

/** One player eligible to receive a Smart Fill offer. */
public interface SmartFillCandidateProjection {
  UUID getUserId();
  String getFirstName();
}
