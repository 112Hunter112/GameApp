package com.parth.sportsapp.sportsbackend.repository;

import com.parth.sportsapp.sportsbackend.model.Notification;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
 * Integration test for {@link NotificationRepository} against real Postgres
 * (Testcontainers, requires Docker). The notification table is append-only and
 * per-user hot (badge polling + feed paging), so the bulk mark-all-read and
 * the paging order are worth pinning down at the database level.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
class NotificationRepositoryIT {

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

  @Autowired private NotificationRepository repository;
  @Autowired private UserRepository userRepository;
  @Autowired private TestEntityManager em;

  private User alice;
  private User bob;

  @BeforeEach
  void setUp() {
    alice = saveUser("alice");
    bob = saveUser("bob");
  }

  @Test
  void markAllReadOnlyTouchesThatUserAndReportsTheCount() {
    save(notification(alice, false));
    save(notification(alice, false));
    save(notification(alice, false));
    save(notification(alice, true));  // already read — must not be counted
    save(notification(bob, false));
    save(notification(bob, false));
    em.flush();

    int updated = repository.markAllReadForUser(alice.getId());
    em.clear(); // bulk JPQL bypasses the persistence context — drop stale state

    assertThat(updated).isEqualTo(3);
    assertThat(repository.countByRecipient_IdAndIsReadFalse(alice.getId())).isZero();
    // Bob's unread badge must be untouched by Alice's mark-all.
    assertThat(repository.countByRecipient_IdAndIsReadFalse(bob.getId())).isEqualTo(2);
  }

  @Test
  void markAllReadIsIdempotent() {
    save(notification(alice, false));
    em.flush();

    assertThat(repository.markAllReadForUser(alice.getId())).isEqualTo(1);
    assertThat(repository.markAllReadForUser(alice.getId())).isZero();
  }

  @Test
  void feedPagesNewestFirst() {
    LocalDateTime now = LocalDateTime.now();
    Notification oldest = save(notification(alice, false, now.minusHours(3)));
    Notification middle = save(notification(alice, false, now.minusHours(2)));
    Notification newest = save(notification(alice, false, now.minusHours(1)));
    save(notification(bob, false, now)); // someone else's — must never leak in
    em.flush();

    Page<Notification> firstPage =
        repository.findByRecipient_IdOrderByCreatedAtDesc(alice.getId(), PageRequest.of(0, 2));

    assertThat(firstPage.getTotalElements()).isEqualTo(3);
    assertThat(firstPage.getContent())
        .extracting(Notification::getId)
        .containsExactly(newest.getId(), middle.getId());

    Page<Notification> secondPage =
        repository.findByRecipient_IdOrderByCreatedAtDesc(alice.getId(), PageRequest.of(1, 2));
    assertThat(secondPage.getContent())
        .extracting(Notification::getId)
        .containsExactly(oldest.getId());
  }

  // --- helpers ---------------------------------------------------------------

  private Notification save(Notification n) {
    return repository.save(n);
  }

  private Notification notification(User recipient, boolean read) {
    return notification(recipient, read, LocalDateTime.now());
  }

  private Notification notification(User recipient, boolean read, LocalDateTime createdAt) {
    Notification n = new Notification();
    n.setRecipient(recipient);
    n.setType("SYSTEM_ALERT");
    n.setMessage("hello");
    n.setReferenceId(UUID.randomUUID());
    n.setRead(read);
    n.setCreatedAt(createdAt);
    return n;
  }

  private User saveUser(String tag) {
    User u = new User();
    u.setFirstName(tag);
    u.setLastName("Test");
    u.setEmail(tag + "-" + System.nanoTime() + "@example.com");
    u.setPassword("placeholder");
    u.setRole(UserRole.USER);
    u.setJoiningDate(LocalDateTime.now());
    u.setUpdatedAt(LocalDateTime.now());
    u.setVerified(true);
    return userRepository.save(u);
  }
}
