package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.RefreshToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Integration test for {@link RefreshTokenRepository} against a real Postgres
 * (via Testcontainers). Verifies the custom bulk-revoke and delete-expired
 * queries actually do what they claim at the database level. Requires Docker.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
class RefreshTokenRepositoryIT {

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

  @Autowired
  private RefreshTokenRepository repository;

  @Test
  void savesAndFindsByHash() {
    RefreshToken t = token(UUID.randomUUID(), "hash-1", Instant.now().plusSeconds(3600), false);
    repository.save(t);

    assertThat(repository.findByTokenHash("hash-1")).isPresent();
    assertThat(repository.findByTokenHash("does-not-exist")).isEmpty();
  }

  @Test
  void revokeAllForUserOnlyAffectsThatUser() {
    UUID userA = UUID.randomUUID();
    UUID userB = UUID.randomUUID();
    repository.save(token(userA, "a1", Instant.now().plusSeconds(3600), false));
    repository.save(token(userA, "a2", Instant.now().plusSeconds(3600), false));
    repository.save(token(userB, "b1", Instant.now().plusSeconds(3600), false));

    int revoked = repository.revokeAllForUser(userA);

    assertThat(revoked).isEqualTo(2);
    assertThat(repository.findByTokenHash("a1").get().isRevoked()).isTrue();
    assertThat(repository.findByTokenHash("a2").get().isRevoked()).isTrue();
    assertThat(repository.findByTokenHash("b1").get().isRevoked()).isFalse();
  }

  @Test
  void deleteExpiredRemovesOnlyPastTokens() {
    repository.save(token(UUID.randomUUID(), "fresh", Instant.now().plusSeconds(3600), false));
    repository.save(token(UUID.randomUUID(), "stale", Instant.now().minusSeconds(3600), false));

    int deleted = repository.deleteExpired(Instant.now());

    assertThat(deleted).isEqualTo(1);
    assertThat(repository.findByTokenHash("fresh")).isPresent();
    assertThat(repository.findByTokenHash("stale")).isEmpty();
  }

  private RefreshToken token(UUID userId, String hash, Instant expiry, boolean revoked) {
    RefreshToken t = new RefreshToken();
    t.setUserId(userId);
    t.setTokenHash(hash);
    t.setExpiryDate(expiry);
    t.setRevoked(revoked);
    t.setCreatedAt(Instant.now());
    return t;
  }
}
