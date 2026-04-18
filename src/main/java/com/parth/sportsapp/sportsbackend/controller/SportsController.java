package com.parth.sportsapp.sportsbackend.controller;

import com.parth.sportsapp.sportsbackend.dto.SportsDto;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.service.SportsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sports")
public class SportsController {

    @Autowired
    private SportsService sportsService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SportsDto>> getActiveSports() {
        List<Sports> sports = sportsService.getAllActiveSports();
        List<SportsDto> dtos = sports.stream()
            .map(s -> new SportsDto(s.getId(), s.getSportName(), s.getScoringType().name()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
