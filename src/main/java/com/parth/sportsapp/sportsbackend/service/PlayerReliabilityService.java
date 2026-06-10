package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.model.BookingStatus;
import com.parth.sportsapp.sportsbackend.repository.BookingRepository;
import com.parth.sportsapp.sportsbackend.repository.UserStatusCountProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Network-level player reliability, derived live from booking history.
 *
 * <p>This is the data CourtReserve-style per-club tools can't have: a player's
 * no-show behavior follows them across every venue on the platform. Vendors
 * see a tier next to each upcoming booking and can require prepayment from
 * low-reliability players (future).</p>
 *
 * <p>Computed on demand rather than stored — no counter-drift, and the
 * definition can evolve without a migration.</p>
 */
@Service
public class PlayerReliabilityService {

  /** Below this many completed data points a player is simply NEW. */
  private static final int MIN_HISTORY = 3;

  public enum Tier { NEW, EXCELLENT, GOOD, FAIR, POOR }

  /** Immutable per-player snapshot. */
  public record Reliability(long totalBookings, long noShows, long cancellations,
                            double noShowRate, Tier tier) {

    static Reliability of(long total, long noShows, long cancellations) {
      // Rate over bookings that reached a final state the player controlled.
      long relevant = total;
      double rate = relevant == 0 ? 0.0 : (double) noShows / relevant;
      Tier tier;
      if (relevant < MIN_HISTORY)      tier = Tier.NEW;
      else if (rate <= 0.02)           tier = Tier.EXCELLENT;
      else if (rate <= 0.08)           tier = Tier.GOOD;
      else if (rate <= 0.20)           tier = Tier.FAIR;
      else                             tier = Tier.POOR;
      return new Reliability(total, noShows, cancellations, rate, tier);
    }

    static Reliability empty() {
      return new Reliability(0, 0, 0, 0.0, Tier.NEW);
    }
  }

  private final BookingRepository bookingRepository;

  public PlayerReliabilityService(BookingRepository bookingRepository) {
    this.bookingRepository = bookingRepository;
  }

  /**
   * Reliability for a batch of players in ONE query — call this with all the
   * user ids on a schedule page rather than once per row.
   * Users with no booking history map to {@link Reliability#empty()}.
   */
  @Transactional(readOnly = true)
  public Map<UUID, Reliability> forUsers(Collection<UUID> userIds) {
    Map<UUID, Reliability> out = new HashMap<>();
    if (userIds == null || userIds.isEmpty()) return out;

    // Accumulate raw counts per user.
    Map<UUID, long[]> counts = new HashMap<>(); // [total, noShows, cancels]
    for (UserStatusCountProjection row : bookingRepository.countStatusesForUsers(userIds)) {
      long[] c = counts.computeIfAbsent(row.getUserId(), k -> new long[3]);
      long n = row.getCnt() == null ? 0 : row.getCnt();
      c[0] += n;
      if (row.getStatus() == BookingStatus.NO_SHOW)   c[1] += n;
      if (row.getStatus() == BookingStatus.CANCELLED) c[2] += n;
    }

    for (UUID id : userIds) {
      long[] c = counts.get(id);
      out.put(id, c == null ? Reliability.empty() : Reliability.of(c[0], c[1], c[2]));
    }
    return out;
  }
}
