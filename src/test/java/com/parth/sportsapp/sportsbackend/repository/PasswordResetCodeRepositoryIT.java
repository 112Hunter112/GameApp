package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.PasswordResetCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Integration test for {@link PasswordResetCodeRepository} against real
 * Postgres (Testcontainers, requires Docker). The reset flow's security rests
 * on these queries being exact: the bulk invalidation scoping, the
 * newest-unused selection, and the rate-limit window count.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
class PasswordResetCodeRepositoryIT {

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

  @Autowired private PasswordResetCodeRepository repository;
  @Autowired private TestEntityManager em;

  @Test
  void invalidateAllOnlyTouchesThatUser() {
    UUID alice = UUID.randomUUID();
    UUID bob = UUID.randomUUID();
    save(code(alice));
    save(code(alice));
    save(code(bob));
    em.flush();

    repository.invalidateAllForUser(alice);
    em.clear(); // bulk JPQL bypasses the persistence context

    // Alice has no live code left; Bob's is untouched.
    assertThat(repository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(alice)).isEmpty();
    assertThat(repository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(bob)).isPresent();
  }

  @Test
  void newestUnusedCodeWinsOverOlderOnes() {
    UUID user = UUID.randomUUID();
    PasswordResetCode older = save(code(user));
    PasswordResetCode newer = save(code(user));
    em.flush();

    // @CreationTimestamp stamps both at insert; separate them explicitly so
    // the ORDER BY has something unambiguous to sort.
    backdate(older, LocalDateTime.now().minusMinutes(10));
    em.clear();

    assertThat(repository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user))
        .isPresent()
        .get()
        .extracting(PasswordResetCode::getId)
        .isEqualTo(newer.getId());
  }

  @Test
  void usedCodesAreInvisibleToTheActiveLookup() {
    UUID user = UUID.randomUUID();
    PasswordResetCode c = code(user);
    c.setUsed(true);
    save(c);
    em.flush();

    assertThat(repository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user)).isEmpty();
  }

  @Test
  void rateLimitWindowCountsOnlyRecentCodes() {
    UUID user = UUID.randomUUID();
    PasswordResetCode stale = save(code(user));
    save(code(user));
    save(code(user));
    em.flush();

    // Push one code outside the 1-hour window.
    backdate(stale, LocalDateTime.now().minusHours(2));
    em.clear();

    long recent = repository.countByUserIdAndCreatedAtAfter(
        user, LocalDateTime.now().minusHours(1));

    // The email-bomb guard must see 2, not 3 — old requests age out.
    assertThat(recent).isEqualTo(2);
  }

  // --- helpers ---------------------------------------------------------------

  private PasswordResetCode save(PasswordResetCode c) {
    return repository.save(c);
  }

  /** created_at is @CreationTimestamp/updatable=false, so backdating goes via SQL. */
  private void backdate(PasswordResetCode c, LocalDateTime to) {
    em.getEntityManager()
        .createNativeQuery("UPDATE password_reset_codes SET created_at = :t WHERE id = :id")
        .setParameter("t", to)
        .setParameter("id", c.getId())
        .executeUpdate();
  }

  private PasswordResetCode code(UUID userId) {
    PasswordResetCode c = new PasswordResetCode();
    c.setUserId(userId);
    c.setCodeHash("a".repeat(64));
    c.setExpiresAt(LocalDateTime.now().plusMinutes(PasswordResetCode.EXPIRY_MINUTES));
    return c;
  }
}
