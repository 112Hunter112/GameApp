package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.SmartFillRequest;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.Courts;
import com.parth.sportsapp.sportsbackend.model.SmartFillOffer;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.CourtRepository;
import com.parth.sportsapp.sportsbackend.repository.SmartFillCandidateProjection;
import com.parth.sportsapp.sportsbackend.repository.SmartFillOfferRepository;
import com.parth.sportsapp.sportsbackend.repository.UserPreferenceRepository;
import com.parth.sportsapp.sportsbackend.repository.UserRepository;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Smart Fill — the dead-hour auto-fill.
 *
 * <p>A vendor has an empty court at an off-peak time. Instead of an empty
 * calendar cell, the platform finds players who (a) play this court's sport,
 * (b) are open to matchmaking, (c) are available on this day type, and (d)
 * were last seen within their notification radius of the venue — and pushes
 * them an in-app offer. The closest players are notified first.</p>
 *
 * <p>This is the feature an Excel-with-a-calendar can't copy: it needs the
 * player preference + location graph that only the two-sided platform has.</p>
 */
@Service
public class SmartFillService {

  private static final Logger log = LoggerFactory.getLogger(SmartFillService.class);

  /** Cap per offer so a vendor can't spam the whole user base. */
  static final int MAX_CANDIDATES = 50;

  /** Max DISTINCT slots a single court may offer per day (cooldown). */
  static final int MAX_SLOTS_PER_COURT_PER_DAY = 3;

  private static final DateTimeFormatter SLOT_FMT =
      DateTimeFormatter.ofPattern("EEE d MMM, HH:mm");

  private final CourtRepository courtRepository;
  private final UserPreferenceRepository userPreferenceRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  private final SmartFillOfferRepository offerRepository;

  public SmartFillService(CourtRepository courtRepository,
                          UserPreferenceRepository userPreferenceRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          SmartFillOfferRepository offerRepository) {
    this.courtRepository = courtRepository;
    this.userPreferenceRepository = userPreferenceRepository;
    this.userRepository = userRepository;
    this.notificationService = notificationService;
    this.offerRepository = offerRepository;
  }

  /**
   * Find matching nearby players and send each an in-app offer.
   *
   * @return how many players were notified
   */
  @Transactional
  public int offerSlot(UUID vendorId, UUID courtId, SmartFillRequest request) {
    // --- validate the slot --------------------------------------------------
    LocalDateTime start = request.getSlotStart();
    LocalDateTime end = request.getSlotEnd();
    if (start == null || end == null || !end.isAfter(start)) {
      throw new BadRequestException("Invalid slot window");
    }
    if (start.isBefore(LocalDateTime.now())) {
      throw new BadRequestException("Slot must be in the future");
    }
    if (start.plusHours(12).isBefore(end)) {
      throw new BadRequestException("Slot too long — max 12 hours");
    }

    // --- authorize: court must belong to one of the vendor's venues ---------
    Courts court = courtRepository.findById(courtId)
        .orElseThrow(() -> new NotFoundException("Court not found"));
    UUID ownerId = court.getVenue().getOwner().getId();
    if (!ownerId.equals(vendorId)) {
      throw new ForbiddenException("This court is not at one of your venues");
    }

    Point venueLocation = court.getVenue().getLocation();
    if (venueLocation == null) {
      throw new BadRequestException("Venue has no location set — Smart Fill needs it for targeting");
    }
    if (court.getSports() == null) {
      throw new BadRequestException("Court has no sport configured");
    }

    // --- anti-spam: cooldown -------------------------------------------------
    // Re-offering the SAME slot is allowed (dedup below means it only reaches
    // new players). But a court may open at most N distinct slots per day.
    List<UUID> alreadyNotified = offerRepository.findNotifiedUserIds(courtId, start);
    boolean slotAlreadyOffered = !alreadyNotified.isEmpty();
    if (!slotAlreadyOffered) {
      long slotsToday = offerRepository.countDistinctSlotsSince(
          courtId, LocalDateTime.now().toLocalDate().atStartOfDay());
      if (slotsToday >= MAX_SLOTS_PER_COURT_PER_DAY) {
        throw new BadRequestException(
            "Daily Smart Fill limit reached for this court ("
                + MAX_SLOTS_PER_COURT_PER_DAY + " slots/day)");
      }
    }

    // --- find candidates -----------------------------------------------------
    DayOfWeek day = start.getDayOfWeek();
    boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;

    List<SmartFillCandidateProjection> candidates =
        userPreferenceRepository.findSmartFillCandidates(
            court.getSports().getId(),
            venueLocation.getY(),   // lat
            venueLocation.getX(),   // lng
            isWeekend,
            MAX_CANDIDATES);

    // --- anti-spam: dedup — drop players already notified for this slot ------
    var alreadySet = new java.util.HashSet<>(alreadyNotified);
    candidates = candidates.stream()
        .filter(c -> !alreadySet.contains(c.getUserId()))
        .toList();

    if (candidates.isEmpty()) {
      log.info("SmartFill: no (new) candidates for court {} at {}", courtId, start);
      return 0;
    }

    // --- build the offer message ---------------------------------------------
    String discount = (request.getDiscountPercent() != null && request.getDiscountPercent() > 0)
        ? " — " + request.getDiscountPercent() + "% off"
        : "";
    String message = String.format("%s at %s is open %s%s. Tap to book!",
        court.getSports().getSportName(),
        court.getVenue().getName(),
        SLOT_FMT.format(start),
        discount);

    // --- notify + record each offer (dedup/cooldown/analytics source) ---------
    int sent = 0;
    for (SmartFillCandidateProjection c : candidates) {
      User recipient = userRepository.findById(c.getUserId()).orElse(null);
      if (recipient == null) continue;
      notificationService.sendSmartFillOffer(recipient, courtId, message);

      SmartFillOffer offer = new SmartFillOffer();
      offer.setCourtId(courtId);
      offer.setUserId(recipient.getId());
      offer.setSlotStart(start);
      offer.setSlotEnd(end);
      offerRepository.save(offer);

      sent++;
    }
    log.info("SmartFill: notified {} players for court {} at {}", sent, courtId, start);
    return sent;
  }
}
