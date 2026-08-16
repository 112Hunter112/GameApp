package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.NotificationResponse;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.Booking;
import com.parth.sportsapp.sportsbackend.model.BookingStatus;
import com.parth.sportsapp.sportsbackend.model.Courts;
import com.parth.sportsapp.sportsbackend.model.Notification;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationRepository notificationRepository;

  @InjectMocks private NotificationService service;

  private User recipient;
  private User sender;

  @BeforeEach
  void setUp() {
    recipient = user("Parth", "Aditya");
    sender = user("Keshav", "Aditya");
  }

  // --- markAsRead (IDOR guard) ------------------------------------------------

  @Test
  void markAsReadFlagsOwnNotification() {
    Notification n = notification(recipient, sender, "MATCH_INVITE");
    when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));

    service.markAsRead(n.getId(), recipient.getId());

    assertThat(n.isRead()).isTrue();
    verify(notificationRepository).save(n);
  }

  @Test
  void markAsReadRejectsUnknownNotification() {
    UUID id = UUID.randomUUID();
    when(notificationRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.markAsRead(id, recipient.getId()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void markAsReadRejectsSomeoneElsesNotification() {
    // The IDOR guard: knowing another user's notification id must not let you
    // touch it.
    Notification n = notification(recipient, sender, "MATCH_INVITE");
    when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));

    assertThatThrownBy(() -> service.markAsRead(n.getId(), UUID.randomUUID()))
        .isInstanceOf(ForbiddenException.class);

    assertThat(n.isRead()).isFalse();
    verify(notificationRepository, never()).save(any());
  }

  @Test
  void markAsReadRejectsNotificationWithoutRecipient() {
    Notification n = notification(null, sender, "SYSTEM_ALERT");
    when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));

    assertThatThrownBy(() -> service.markAsRead(n.getId(), recipient.getId()))
        .isInstanceOf(ForbiddenException.class);
  }

  // --- feed --------------------------------------------------------------------

  @Test
  void getMyNotificationsMapsEntitiesToDtos() {
    Notification withSender = notification(recipient, sender, "FRIEND_REQUEST");
    withSender.setMessage("Keshav Aditya sent you a friend request.");
    Notification system = notification(recipient, null, "BOOKING_CONFIRMED");

    Pageable page = PageRequest.of(0, 20);
    when(notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(recipient.getId(), page))
        .thenReturn(new PageImpl<>(List.of(withSender, system), page, 2));

    Page<NotificationResponse> res = service.getMyNotifications(recipient.getId(), page);

    assertThat(res.getTotalElements()).isEqualTo(2);

    NotificationResponse first = res.getContent().get(0);
    assertThat(first.getId()).isEqualTo(withSender.getId());
    assertThat(first.getType()).isEqualTo("FRIEND_REQUEST");
    assertThat(first.getMessage()).isEqualTo("Keshav Aditya sent you a friend request.");
    assertThat(first.getSenderId()).isEqualTo(sender.getId());
    assertThat(first.getSenderName()).isEqualTo("Keshav Aditya");
    assertThat(first.getSenderAvatarUrl()).isEqualTo("http://cdn/keshav.png");

    // System notifications have no sender — the DTO must not blow up on null.
    NotificationResponse second = res.getContent().get(1);
    assertThat(second.getSenderId()).isNull();
    assertThat(second.getSenderName()).isNull();
  }

  @Test
  void unreadCountAndMarkAllReadDelegateToRepository() {
    when(notificationRepository.countByRecipient_IdAndIsReadFalse(recipient.getId())).thenReturn(7L);
    assertThat(service.getUnreadCount(recipient.getId())).isEqualTo(7L);

    when(notificationRepository.markAllReadForUser(recipient.getId())).thenReturn(3);
    assertThat(service.markAllAsRead(recipient.getId())).isEqualTo(3);
  }

  // --- match & friend notifications ---------------------------------------------

  @Test
  void sendMatchInviteStoresUnreadNotificationForRecipient() {
    UUID matchId = UUID.randomUUID();

    service.sendMatchInvite(recipient, sender, matchId);

    Notification saved = captureSaved();
    assertThat(saved.getRecipient()).isSameAs(recipient);
    assertThat(saved.getSender()).isSameAs(sender);
    assertThat(saved.getType()).isEqualTo("MATCH_INVITE");
    assertThat(saved.getReferenceId()).isEqualTo(matchId);
    assertThat(saved.getMessage()).contains("Keshav Aditya", "logged a match result");
    assertThat(saved.isRead()).isFalse();
  }

  @Test
  void matchLifecycleAlertsAreSystemNotifications() {
    UUID matchId = UUID.randomUUID();

    service.sendMatchVerified(recipient, matchId);
    Notification verified = captureSaved();
    assertThat(verified.getType()).isEqualTo("MATCH_VERIFIED");
    assertThat(verified.getSender()).isNull(); // system alert, no human sender
    assertThat(verified.getReferenceId()).isEqualTo(matchId);
  }

  @Test
  void friendRequestAndAcceptNotifyTheRightSides() {
    UUID friendshipId = UUID.randomUUID();

    // Request: receiver gets it, requester is the sender.
    service.sendFriendRequest(recipient, sender, friendshipId);
    Notification request = captureSaved();
    assertThat(request.getType()).isEqualTo("FRIEND_REQUEST");
    assertThat(request.getRecipient()).isSameAs(recipient);
    assertThat(request.getSender()).isSameAs(sender);
    assertThat(request.getMessage()).contains("Keshav Aditya", "friend request");

    // Accept: the ORIGINAL REQUESTER gets it, accepter is the sender.
    service.sendFriendAccepted(sender, recipient, friendshipId);
    Notification accepted = lastSaved(2);
    assertThat(accepted.getType()).isEqualTo("FRIEND_ACCEPTED");
    assertThat(accepted.getRecipient()).isSameAs(sender);
    assertThat(accepted.getSender()).isSameAs(recipient);
    assertThat(accepted.getMessage()).contains("Parth Aditya", "accepted");
  }

  // --- booking notifications ------------------------------------------------------

  @Test
  void bookingCreatedDistinguishesInstantBookFromRequest() {
    Booking pending = booking(BookingStatus.PENDING);
    service.sendBookingCreated(recipient, sender, pending);
    Notification requested = captureSaved();
    assertThat(requested.getType()).isEqualTo("BOOKING_REQUESTED");
    assertThat(requested.getMessage()).contains("requested", "Tap to confirm or decline");
    assertThat(requested.getReferenceId()).isEqualTo(pending.getId());

    Booking confirmed = booking(BookingStatus.CONFIRMED);
    service.sendBookingCreated(recipient, sender, confirmed);
    Notification created = lastSaved(2);
    assertThat(created.getType()).isEqualTo("BOOKING_CREATED");
    assertThat(created.getMessage()).contains("booked", "Court 2");
  }

  @Test
  void bookingConfirmedNotifiesPlayerWithVenueCourtAndTime() {
    Booking booking = booking(BookingStatus.CONFIRMED);

    service.sendBookingConfirmed(recipient, booking);

    Notification saved = captureSaved();
    assertThat(saved.getType()).isEqualTo("BOOKING_CONFIRMED");
    assertThat(saved.getSender()).isNull();
    assertThat(saved.getMessage())
        .contains("Riverside Sports Hub", "Court 2", "18:00", "19:30");
  }

  @Test
  void bookingDeclinedIncludesOwnerReasonWhenGiven() {
    Booking withReason = booking(BookingStatus.DECLINED);
    withReason.setCancellationReason("Court flooded");
    service.sendBookingDeclined(recipient, withReason);
    assertThat(captureSaved().getMessage()).contains("declined", "Court flooded");

    Booking noReason = booking(BookingStatus.DECLINED);
    service.sendBookingDeclined(recipient, noReason);
    assertThat(lastSaved(2).getMessage()).contains("declined");
  }

  @Test
  void cancellationsNotifyTheOtherParty() {
    Booking booking = booking(BookingStatus.CANCELLED);

    // Player cancels -> owner is told who did it.
    service.sendBookingCancelledByPlayer(recipient, sender, booking);
    Notification toOwner = captureSaved();
    assertThat(toOwner.getType()).isEqualTo("BOOKING_CANCELLED");
    assertThat(toOwner.getRecipient()).isSameAs(recipient);
    assertThat(toOwner.getSender()).isSameAs(sender);
    assertThat(toOwner.getMessage()).contains("Keshav Aditya", "cancelled");

    // Venue cancels without a reason -> player still gets an apology.
    service.sendBookingCancelledByVenue(sender, booking);
    Notification toPlayer = lastSaved(2);
    assertThat(toPlayer.getType()).isEqualTo("BOOKING_CANCELLED");
    assertThat(toPlayer.getMessage()).contains("Riverside Sports Hub", "Sorry about that!");
  }

  @Test
  void noShowNotificationNamesTheVenueAndOffersRecourse() {
    UUID bookingId = UUID.randomUUID();

    service.sendNoShowMarked(recipient, bookingId, "Riverside Sports Hub");

    Notification saved = captureSaved();
    assertThat(saved.getType()).isEqualTo("NO_SHOW_MARKED");
    assertThat(saved.getReferenceId()).isEqualTo(bookingId);
    // Fairness: the player must learn about the strike AND how to dispute it.
    assertThat(saved.getMessage()).contains("no-show", "Riverside Sports Hub", "contact the venue");
  }

  @Test
  void smartFillOfferDeepLinksToTheCourt() {
    UUID courtId = UUID.randomUUID();

    service.sendSmartFillOffer(recipient, courtId, "A slot just opened near you!");

    Notification saved = captureSaved();
    assertThat(saved.getType()).isEqualTo("SMART_FILL_OFFER");
    assertThat(saved.getReferenceId()).isEqualTo(courtId);
    assertThat(saved.getMessage()).isEqualTo("A slot just opened near you!");
  }

  // --- helpers ---------------------------------------------------------------

  private Notification captureSaved() {
    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    return captor.getValue();
  }

  /** The most recent of {@code expectedSaves} save() calls. */
  private Notification lastSaved(int expectedSaves) {
    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository, org.mockito.Mockito.times(expectedSaves)).save(captor.capture());
    return captor.getAllValues().get(expectedSaves - 1);
  }

  private User user(String first, String last) {
    User u = new User();
    u.setId(UUID.randomUUID());
    u.setFirstName(first);
    u.setLastName(last);
    u.setEmail(first.toLowerCase() + "@example.com");
    u.setProfilePictureUrl("http://cdn/" + first.toLowerCase() + ".png");
    return u;
  }

  private Notification notification(User recipient, User sender, String type) {
    Notification n = new Notification();
    n.setId(UUID.randomUUID());
    n.setRecipient(recipient);
    n.setSender(sender);
    n.setType(type);
    n.setReferenceId(UUID.randomUUID());
    n.setMessage("hello");
    n.setCreatedAt(LocalDateTime.now());
    return n;
  }

  private Booking booking(BookingStatus status) {
    Venue venue = new Venue();
    venue.setId(UUID.randomUUID());
    venue.setName("Riverside Sports Hub");

    Courts court = new Courts();
    court.setId(UUID.randomUUID());
    court.setCourtNumber("Court 2");
    court.setVenue(venue);

    Booking b = new Booking();
    b.setId(UUID.randomUUID());
    b.setCourt(court);
    b.setStatus(status);
    b.setStartTime(LocalDateTime.now().plusDays(3).withHour(18).withMinute(0).withSecond(0));
    b.setEndTime(LocalDateTime.now().plusDays(3).withHour(19).withMinute(30).withSecond(0));
    return b;
  }
}
