package com.parth.sportsapp.sportsbackend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parth.sportsapp.sportsbackend.model.ScoringType;
import com.parth.sportsapp.sportsbackend.model.Sports;
import com.parth.sportsapp.sportsbackend.repository.SportsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Component
public class SportsSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SportsSeeder.class);
  private static final String SEED_PATH = "seed/sports.json";

  private final SportsRepository sportsRepository;
  private final ObjectMapper objectMapper;

  public SportsSeeder(SportsRepository sportsRepository, ObjectMapper objectMapper) {
    this.sportsRepository = sportsRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    List<SportSeed> seeds;
    try (InputStream in = new ClassPathResource(SEED_PATH).getInputStream()) {
      seeds = objectMapper.readValue(in, new TypeReference<List<SportSeed>>() {});
    } catch (Exception e) {
      log.warn("Skipping sports seed: could not read {} ({})", SEED_PATH, e.getMessage());
      return;
    }

    int inserted = 0;
    int skipped = 0;
    for (SportSeed s : seeds) {
      if (s.name == null || s.name.isBlank()) continue;

      if (sportsRepository.existsBySportNameIgnoreCase(s.name)) {
        skipped++;
        continue;
      }

      Sports sport = new Sports();
      sport.setSportName(s.name);
      sport.setMinPlayers(s.minPlayers == null ? 1 : s.minPlayers);
      sport.setMaxPlayers(s.maxPlayers == null ? s.minPlayers == null ? 1 : s.minPlayers : s.maxPlayers);
      sport.setScoringType(parseScoring(s.scoringType));
      sport.setIconURL(s.iconUrl);
      sport.setDescription(s.description);
      sport.setActive(true);
      sportsRepository.save(sport);
      inserted++;
    }
    log.info("Sports seed complete: {} inserted, {} already present", inserted, skipped);
  }

  private ScoringType parseScoring(String raw) {
    if (raw == null) return ScoringType.POINTS;
    try {
      return ScoringType.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Unknown scoringType '{}' in seed file; defaulting to POINTS", raw);
      return ScoringType.POINTS;
    }
  }

  public static class SportSeed {
    public String name;
    public Integer minPlayers;
    public Integer maxPlayers;
    public String scoringType;
    public String iconUrl;
    public String description;
  }
}
