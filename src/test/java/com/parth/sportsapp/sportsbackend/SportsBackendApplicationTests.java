package com.parth.sportsapp.sportsbackend;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Smoke test: verifies the full Spring application context wires up against a
 * real PostGIS database. Redis/Redisson is mocked so only Docker (for PostGIS)
 * is required. Requires Docker to be running.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class SportsBackendApplicationTests {

  @Container
  static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
      DockerImageName.parse("postgis/postgis:15-3.4")
          .asCompatibleSubstituteFor("postgres"))
      .withInitScript("init-postgis.sql");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGIS::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGIS::getUsername);
    registry.add("spring.datasource.password", POSTGIS::getPassword);
  }

  @MockitoBean
  RedissonClient redissonClient;

  @Test
  void contextLoads() {
  }

}
