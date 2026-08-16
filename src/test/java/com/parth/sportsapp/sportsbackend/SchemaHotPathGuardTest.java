package com.parth.sportsapp.sportsbackend;

import com.parth.sportsapp.sportsbackend.model.Booking;
import com.parth.sportsapp.sportsbackend.model.CourtBlock;
import com.parth.sportsapp.sportsbackend.model.Friendship;
import com.parth.sportsapp.sportsbackend.model.Notification;
import com.parth.sportsapp.sportsbackend.model.PasswordResetCode;
import com.parth.sportsapp.sportsbackend.model.RefreshToken;
import com.parth.sportsapp.sportsbackend.model.SmartFillOffer;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the schema declarations behind the hottest query paths.
 *
 * The production schema is Hibernate-managed (ddl-auto: update), so the
 * {@code @Index}/{@code @UniqueConstraint} annotations on the entities ARE the
 * production indexes. Losing one in a refactor or a fork merge silently turns
 * a point lookup into a growing sequential scan — these tests make that loss
 * a build failure instead.
 *
 * Each assertion names the query it protects.
 */
class SchemaHotPathGuardTest {

  @Test
  void bookingConflictCheckIsIndexed() {
    // BookingRepository.countConflicts / findActiveInWindow: probed on every
    // booking attempt and every availability grid, while the court row lock is
    // held — this one keeps the lock hold time flat as bookings grow.
    assertThat(indexColumnSets(Booking.class))
        .contains("court_id,start_time,end_time");
  }

  @Test
  void bookingPlayerViewsAreIndexed() {
    // BookingRepository player views (my upcoming / my past) and the
    // reliability aggregate all filter by user_id.
    assertThat(indexColumnSets(Booking.class))
        .contains("user_id,start_time");
  }

  @Test
  void notificationBadgeAndFeedAreIndexed() {
    Set<String> indexes = indexColumnSets(Notification.class);

    // countByRecipient_IdAndIsReadFalse — the bell badge, polled by clients.
    assertThat(indexes).contains("recipient_id,is_read");
    // findByRecipient_IdOrderByCreatedAtDesc — the paged feed.
    assertThat(indexes).contains("recipient_id,created_at");
  }

  @Test
  void courtBlockOverlapCheckIsIndexed() {
    // CourtBlockRepository.countOverlapping/findInWindow — runs inside the
    // booking transaction under the court lock.
    assertThat(indexColumnSets(CourtBlock.class))
        .contains("court_id,start_time,end_time");
  }

  @Test
  void passwordResetLookupsAreIndexed() {
    // Rate-limit count and newest-unused-code lookup, both by user + recency.
    assertThat(indexColumnSets(PasswordResetCode.class))
        .contains("user_id,created_at");
  }

  @Test
  void matchDateOrderingIsIndexed() {
    // Every per-user match query orders by match_date; without this the heavy
    // history tail (20k-match players) sorts the whole set each page.
    assertThat(indexColumnSets(com.parth.sportsapp.sportsbackend.model.Match.class))
        .contains("match_date");
  }

  @Test
  void participantsUserLookupsAreIndexed() {
    // Every MatchRepository per-user query joins participants on user_id; the
    // composite PK (match_id, user_id) cannot serve that predicate.
    assertThat(indexColumnSets(com.parth.sportsapp.sportsbackend.model.Participants.class))
        .contains("user_id,match_id");
  }

  @Test
  void friendshipReceiverSideIsIndexed() {
    // Pending-request list and badge count both probe by receiver + status;
    // the unique (requester_id, receiver_id) constraint covers neither.
    assertThat(indexColumnSets(Friendship.class))
        .contains("receiver_id,status");
  }

  @Test
  void feedShareLookupsAreIndexedAndDeduped() {
    // The feed reads shares by author newest-first; target cleanup probes by
    // (target_type, target_id); one share per user per target is a DB rule.
    Set<String> indexes =
        indexColumnSets(com.parth.sportsapp.sportsbackend.model.FeedShare.class);
    assertThat(indexes).contains("user_id,created_at");
    assertThat(indexes).contains("target_type,target_id");
    assertThat(uniqueConstraintColumnSets(
        com.parth.sportsapp.sportsbackend.model.FeedShare.class))
        .contains("user_id,target_type,target_id");
  }

  @Test
  void refreshTokenHashLookupIsUniqueAndIndexed() {
    // Every authenticated refresh resolves a token by its hash; uniqueness is
    // also what makes rotation-reuse detection trustworthy.
    Table table = tableOf(RefreshToken.class);
    boolean uniqueHashIndex = Arrays.stream(table.indexes())
        .anyMatch(i -> normalize(i.columnList()).equals("token_hash") && i.unique());
    assertThat(uniqueHashIndex)
        .as("refresh_tokens needs a UNIQUE index on token_hash")
        .isTrue();
  }

  @Test
  void friendshipPairIsUniqueAtTheDatabaseLevel() {
    // Two concurrent friend requests between the same pair must collapse to
    // one row — application checks alone cannot guarantee that.
    assertThat(uniqueConstraintColumnSets(Friendship.class))
        .contains("requester_id,receiver_id");
  }

  @Test
  void smartFillOffersCarryADbLevelDedupeConstraint() {
    // Anti-spam relies on the DB rejecting duplicate offers, not just the
    // service-level check.
    assertThat(tableOf(SmartFillOffer.class).uniqueConstraints()).isNotEmpty();
  }

  // --- helpers ---------------------------------------------------------------

  private static Table tableOf(Class<?> entity) {
    Table table = entity.getAnnotation(Table.class);
    assertThat(table).as(entity.getSimpleName() + " must declare @Table").isNotNull();
    return table;
  }

  private static Set<String> indexColumnSets(Class<?> entity) {
    return Arrays.stream(tableOf(entity).indexes())
        .map(Index::columnList)
        .map(SchemaHotPathGuardTest::normalize)
        .collect(Collectors.toSet());
  }

  private static Set<String> uniqueConstraintColumnSets(Class<?> entity) {
    return Arrays.stream(tableOf(entity).uniqueConstraints())
        .map(UniqueConstraint::columnNames)
        .map(cols -> Arrays.stream(cols)
            .map(SchemaHotPathGuardTest::normalize)
            .collect(Collectors.joining(",")))
        .collect(Collectors.toSet());
  }

  /** "recipient_id, is_read" -> "recipient_id,is_read" for comparison. */
  private static String normalize(String columnList) {
    return columnList.replace(" ", "").toLowerCase();
  }
}
