package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.repository.CourtRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourtService {

  @Autowired
  private CourtRepository courtRepository;


}
