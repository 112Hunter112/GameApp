package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.Notification;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

  @Autowired
  private NotificationRepository notificationRepository;

  /**
   * Creates a notification for a match invite/log.
   * Called by MatchService when a user adds an opponent.
   */
  public void sendMatchInvite(User recipient, User sender, UUID matchId) {
    Notification notification = new Notification();

    notification.setRecipient(recipient);
    notification.setSender(sender);
    notification.setType("MATCH_INVITE");
    notification.setReferenceId(matchId);

    // Custom message logic
    String msg = sender.getFirstName() + " " + sender.getLastName() +
        " logged a match result against you.";
    notification.setMessage(msg);

    notificationRepository.save(notification);

    // TODO (Future): Add WebSocket/Firebase trigger here for real-time popups
    // pushNotificationService.send(recipient.getId(), msg);
  }

  /**
   * Marks a notification as read when the user clicks it.
   *
   * @param notificationId the notification to mark
   * @param currentUserId  the authenticated caller — must own the notification
   * @throws NotFoundException  if the notification does not exist
   * @throws ForbiddenException if the notification belongs to a different user (IDOR guard)
   */
  public void markAsRead(UUID notificationId, UUID currentUserId) {
    Notification n = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new NotFoundException("Notification not found"));

    if (n.getRecipient() == null || !n.getRecipient().getId().equals(currentUserId)) {
      // Do not reveal whether the notification exists for another user.
      throw new ForbiddenException("You cannot modify this notification");
    }

    n.setRead(true);
    notificationRepository.save(n);
  }


  /**
   * 2. MATCH VERIFIED
   * Called when an opponent accepts the result. Notifies the Creator.
   */
  public void sendMatchVerified(User recipient, UUID matchId) {
    String msg = "Great news! Your match result has been verified.";
    // Sender is null because this is a System Alert
    saveNotification(recipient, null, "MATCH_VERIFIED", matchId, msg);
  }

  /**
   * 3. MATCH REJECTED
   * Called when an opponent disputes the result. Notifies the Creator.
   */
  public void sendMatchRejected(User recipient, UUID matchId) {
    String msg = "Action Required: An opponent disputed your match result.";
    saveNotification(recipient, null, "MATCH_REJECTED", matchId, msg);
  }

  /**
   * 4. SCORE UPDATED
   * Called when a score is edited. Notifies all participants to re-verify.
   */
  public void sendScoreUpdated(User recipient, UUID matchId) {
    String msg = "Update: The score for one of your matches was changed.";
    saveNotification(recipient, null, "SCORE_UPDATED", matchId, msg);
  }



  /**
   * Smart Fill: tell a nearby matching player that a court slot just opened
   * up at a venue near them. referenceId = the court, so the client can deep
   * link straight into the booking flow.
   */
  public void sendSmartFillOffer(User recipient, UUID courtId, String message) {
    saveNotification(recipient, null, "SMART_FILL_OFFER", courtId, message);
  }

  /**
   * Fairness: the player must always know when a venue marked them as a
   * no-show — their reliability score is affected, so silent marking would
   * be a trust violation. referenceId = the booking.
   */
  public void sendNoShowMarked(User recipient, UUID bookingId, String venueName) {
    String msg = "You were marked as a no-show for your booking at " + venueName
        + ". If this is a mistake, please contact the venue.";
    saveNotification(recipient, null, "NO_SHOW_MARKED", bookingId, msg);
  }

  // ===========================================================================
  // FEED (notifications screen + bell badge)
  // ===========================================================================

  /** Paged feed, newest first, as DTOs (never expose the raw entity/users). */
  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public org.springframework.data.domain.Page<com.parth.sportsapp.sportsbackend.dto.NotificationResponse>
      getMyNotifications(UUID userId, org.springframework.data.domain.Pageable pageable) {
    return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(userId, pageable)
        .map(this::toResponse);
  }

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public long getUnreadCount(UUID userId) {
    return notificationRepository.countByRecipient_IdAndIsReadFalse(userId);
  }

  @org.springframework.transaction.annotation.Transactional
  public int markAllAsRead(UUID userId) {
    return notificationRepository.markAllReadForUser(userId);
  }

  private com.parth.sportsapp.sportsbackend.dto.NotificationResponse toResponse(Notification n) {
    com.parth.sportsapp.sportsbackend.dto.NotificationResponse r =
        new com.parth.sportsapp.sportsbackend.dto.NotificationResponse();
    r.setId(n.getId());
    r.setType(n.getType());
    r.setMessage(n.getMessage());
    r.setReferenceId(n.getReferenceId());
    r.setRead(n.isRead());
    r.setCreatedAt(n.getCreatedAt());
    if (n.getSender() != null) {
      r.setSenderId(n.getSender().getId());
      r.setSenderName(n.getSender().getFirstName() + " " + n.getSender().getLastName());
      r.setSenderAvatarUrl(n.getSender().getProfilePictureUrl());
    }
    return r;
  }

  // ===========================================================================
  // FRIENDS
  // ===========================================================================

  /** Someone sent a friend request. referenceId = friendship id. */
  public void sendFriendRequest(User receiver, User requester, UUID friendshipId) {
    String msg = requester.getFirstName() + " " + requester.getLastName() +
        " sent you a friend request.";
    saveNotification(receiver, requester, "FRIEND_REQUEST", friendshipId, msg);
  }

  /** A request you sent was accepted. Notifies the original requester. */
  public void sendFriendAccepted(User requester, User accepter, UUID friendshipId) {
    String msg = accepter.getFirstName() + " " + accepter.getLastName() +
        " accepted your friend request. You're now friends!";
    saveNotification(requester, accepter, "FRIEND_ACCEPTED", friendshipId, msg);
  }

  // ===========================================================================
  // BOOKINGS
  // ===========================================================================

  /**
   * New booking landed (instant-book) or was requested (request-to-book).
   * Notifies the venue owner either way — they always want to know.
   */
  public void sendBookingCreated(User owner, User player, com.parth.sportsapp.sportsbackend.model.Booking booking) {
    String when = formatBookingTime(booking);
    String court = booking.getCourt().getCourtNumber();
    String msg = booking.getStatus() == com.parth.sportsapp.sportsbackend.model.BookingStatus.PENDING
        ? player.getFirstName() + " " + player.getLastName() + " requested " + court + " on " + when + ". Tap to confirm or decline."
        : player.getFirstName() + " " + player.getLastName() + " booked " + court + " on " + when + ".";
    String type = booking.getStatus() == com.parth.sportsapp.sportsbackend.model.BookingStatus.PENDING
        ? "BOOKING_REQUESTED" : "BOOKING_CREATED";
    saveNotification(owner, player, type, booking.getId(), msg);
  }

  /** Owner approved a pending request. Notifies the player. */
  public void sendBookingConfirmed(User player, com.parth.sportsapp.sportsbackend.model.Booking booking) {
    String msg = "Booking confirmed! " + booking.getCourt().getVenue().getName() +
        " — " + booking.getCourt().getCourtNumber() + ", " + formatBookingTime(booking) + ".";
    saveNotification(player, null, "BOOKING_CONFIRMED", booking.getId(), msg);
  }

  /** Owner declined a pending request. Notifies the player. */
  public void sendBookingDeclined(User player, com.parth.sportsapp.sportsbackend.model.Booking booking) {
    String reason = booking.getCancellationReason();
    String msg = "Your booking request at " + booking.getCourt().getVenue().getName() +
        " for " + formatBookingTime(booking) + " was declined" +
        (reason != null ? ": " + reason : ".");
    saveNotification(player, null, "BOOKING_DECLINED", booking.getId(), msg);
  }

  /** Player cancelled. Notifies the venue owner. */
  public void sendBookingCancelledByPlayer(User owner, User player, com.parth.sportsapp.sportsbackend.model.Booking booking) {
    String msg = player.getFirstName() + " " + player.getLastName() + " cancelled their booking of " +
        booking.getCourt().getCourtNumber() + " on " + formatBookingTime(booking) + ".";
    saveNotification(owner, player, "BOOKING_CANCELLED", booking.getId(), msg);
  }

  /** Venue cancelled. Notifies the player. */
  public void sendBookingCancelledByVenue(User player, com.parth.sportsapp.sportsbackend.model.Booking booking) {
    String reason = booking.getCancellationReason();
    String msg = booking.getCourt().getVenue().getName() + " cancelled your booking for " +
        formatBookingTime(booking) + (reason != null ? ": " + reason : ". Sorry about that!");
    saveNotification(player, null, "BOOKING_CANCELLED", booking.getId(), msg);
  }

  private String formatBookingTime(com.parth.sportsapp.sportsbackend.model.Booking booking) {
    java.time.format.DateTimeFormatter day = java.time.format.DateTimeFormatter.ofPattern("EEE d MMM");
    java.time.format.DateTimeFormatter time = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    return booking.getStartTime().format(day) + ", " +
        booking.getStartTime().format(time) + "–" + booking.getEndTime().format(time);
  }

  private void saveNotification(User recipient, User sender, String type, UUID referenceId, String message) {
    Notification notification = new Notification();
    notification.setRecipient(recipient);
    notification.setSender(sender); // Can be null for System messages
    notification.setType(type);
    notification.setReferenceId(referenceId);
    notification.setMessage(message);

    // Default isRead is usually false in Entity, but setting explicitly helps clarity
    notification.setRead(false);

    notificationRepository.save(notification);
  }
}
