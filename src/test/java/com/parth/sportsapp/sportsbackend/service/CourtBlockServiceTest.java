package com.parth.sportsapp.sportsbackend.service;

import com.parth.sportsapp.sportsbackend.dto.CourtBlockRequest;
import com.parth.sportsapp.sportsbackend.dto.CourtBlockResponse;
import com.parth.sportsapp.sportsbackend.exception.BadRequestException;
import com.parth.sportsapp.sportsbackend.exception.ForbiddenException;
import com.parth.sportsapp.sportsbackend.exception.NotFoundException;
import com.parth.sportsapp.sportsbackend.model.CourtBlock;
import com.parth.sportsapp.sportsbackend.model.Courts;
import com.parth.sportsapp.sportsbackend.model.User;
import com.parth.sportsapp.sportsbackend.model.Venue;
import com.parth.sportsapp.sportsbackend.repository.CourtBlockRepository;
import com.parth.sportsapp.sportsbackend.repository.CourtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class CourtBlockServiceTest {

  @Mock private CourtBlockRepository courtBlockRepository;
  @Mock private CourtRepository courtRepository;

  @InjectMocks private CourtBlockService service;

  private User owner;
  private Venue venue;
  private Courts court;

  // A fixed, clearly-future anchor keeps assertions stable.
  private final LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(18).withMinute(0);

  @BeforeEach
  void setUp() {
    owner = new User();
    owner.setId(UUID.randomUUID());
    owner.setFirstName("Owner");

    venue = new Venue();
    venue.setId(UUID.randomUUID());
    venue.setName("Riverside Sports Hub");
    venue.setOwner(owner);

    court = new Courts();
    court.setId(UUID.randomUUID());
    court.setCourtNumber("Court 2");
    court.setVenue(venue);
  }

  // --- createBlock -----------------------------------------------------------

  @Test
  void createBlockSavesAndMapsResponse() {
    when(courtRepository.findById(court.getId())).thenReturn(Optional.of(court));
    when(courtBlockRepository.countOverlapping(court.getId(), start, start.plusHours(2)))
        .thenReturn(0L);
    when(courtBlockRepository.save(any(CourtBlock.class))).thenAnswer(inv -> inv.getArgument(0));

    CourtBlockResponse res =
        service.createBlock(court.getId(), owner.getId(), request(start, start.plusHours(2), "  Resurfacing  "));

    assertThat(res.getCourtId()).isEqualTo(court.getId());
    assertThat(res.getCourtNumber()).isEqualTo("Court 2");
    assertThat(res.getVenueId()).isEqualTo(venue.getId());
    assertThat(res.getVenueName()).isEqualTo("Riverside Sports Hub");
    assertThat(res.getStartTime()).isEqualTo(start);
    assertThat(res.getEndTime()).isEqualTo(start.plusHours(2));
    assertThat(res.getReason()).isEqualTo("Resurfacing"); // trimmed
  }

  @Test
  void createBlockStoresNullForBlankReason() {
    when(courtRepository.findById(court.getId())).thenReturn(Optional.of(court));
    when(courtBlockRepository.countOverlapping(any(), any(), any())).thenReturn(0L);
    when(courtBlockRepository.save(any(CourtBlock.class))).thenAnswer(inv -> inv.getArgument(0));

    CourtBlockResponse res =
        service.createBlock(court.getId(), owner.getId(), request(start, start.plusHours(1), "   "));

    assertThat(res.getReason()).isNull();
  }

  @Test
  void createBlockRejectsUnknownCourt() {
    UUID courtId = UUID.randomUUID();
    when(courtRepository.findById(courtId)).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
        service.createBlock(courtId, owner.getId(), request(start, start.plusHours(1), null)))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void createBlockRejectsNonOwner() {
    when(courtRepository.findById(court.getId())).thenReturn(Optional.of(court));

    assertThatThrownBy(() ->
        service.createBlock(court.getId(), UUID.randomUUID(), request(start, start.plusHours(1), null)))
        .isInstanceOf(ForbiddenException.class);

    verify(courtBlockRepository, never()).save(any());
  }

  @Test
  void createBlockRejectsMissingOrInvertedTimes() {
    when(courtRepository.findById(court.getId())).thenReturn(Optional.of(court));

    // Missing end
    assertThatThrownBy(() ->
        service.createBlock(court.getId(), owner.getId(), request(start, null, null)))
        .isInstanceOf(BadRequestException.class);

    // end == start
    assertThatThrownBy(() ->
        service.createBlock(court.getId(), owner.getId(), request(start, start, null)))
        .isInstanceOf(BadRequestException.class);

    // end before start
    assertThatThrownBy(() ->
        service.createBlock(court.getId(), owner.getId(), request(start, start.minusHours(1), null)))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void createBlockCapsWindowAtThirtyOneDays() {
    when(courtRepository.findById(court.getId())).thenReturn(Optional.of(court));

    assertThatThrownBy(() ->
        service.createBlock(court.getId(), owner.getId(),
            request(start, start.plusDays(31).plusMinutes(1), null)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("31 days");

    // Exactly 31 days is still allowed (boundary).
    when(courtBlockRepository.countOverlapping(any(), any(), any())).thenReturn(0L);
    when(courtBlockRepository.save(any(CourtBlock.class))).thenAnswer(inv -> inv.getArgument(0));
    CourtBlockResponse res = service.createBlock(court.getId(), owner.getId(),
        request(start, start.plusDays(31), null));
    assertThat(res.getEndTime()).isEqualTo(start.plusDays(31));
  }

  @Test
  void createBlockRejectsOverlapWithExistingBlock() {
    when(courtRepository.findById(court.getId())).thenReturn(Optional.of(court));
    when(courtBlockRepository.countOverlapping(court.getId(), start, start.plusHours(1)))
        .thenReturn(1L);

    assertThatThrownBy(() ->
        service.createBlock(court.getId(), owner.getId(), request(start, start.plusHours(1), null)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("overlaps");

    verify(courtBlockRepository, never()).save(any());
  }

  // --- deleteBlock -----------------------------------------------------------

  @Test
  void deleteBlockRemovesOwnBlock() {
    CourtBlock block = block();
    when(courtBlockRepository.findById(block.getId())).thenReturn(Optional.of(block));

    service.deleteBlock(block.getId(), owner.getId());

    verify(courtBlockRepository).delete(block);
  }

  @Test
  void deleteBlockRejectsUnknownBlock() {
    UUID blockId = UUID.randomUUID();
    when(courtBlockRepository.findById(blockId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteBlock(blockId, owner.getId()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void deleteBlockRejectsNonOwner() {
    CourtBlock block = block();
    when(courtBlockRepository.findById(block.getId())).thenReturn(Optional.of(block));

    assertThatThrownBy(() -> service.deleteBlock(block.getId(), UUID.randomUUID()))
        .isInstanceOf(ForbiddenException.class);

    verify(courtBlockRepository, never()).delete(any(CourtBlock.class));
  }

  // --- getOwnerBlocks --------------------------------------------------------

  @Test
  void getOwnerBlocksQueriesTheWholeDayAndMaps() {
    LocalDate day = LocalDate.now().plusDays(2);
    CourtBlock block = block();
    when(courtBlockRepository.findForOwner(
        owner.getId(), venue.getId(), day.atStartOfDay(), day.plusDays(1).atStartOfDay()))
        .thenReturn(List.of(block));

    List<CourtBlockResponse> res = service.getOwnerBlocks(owner.getId(), venue.getId(), day);

    assertThat(res).hasSize(1);
    assertThat(res.get(0).getId()).isEqualTo(block.getId());
    assertThat(res.get(0).getVenueName()).isEqualTo("Riverside Sports Hub");
  }

  // --- helpers ---------------------------------------------------------------

  private CourtBlockRequest request(LocalDateTime start, LocalDateTime end, String reason) {
    CourtBlockRequest r = new CourtBlockRequest();
    r.setStartTime(start);
    r.setEndTime(end);
    r.setReason(reason);
    return r;
  }

  private CourtBlock block() {
    CourtBlock b = new CourtBlock();
    b.setId(UUID.randomUUID());
    b.setCourt(court);
    b.setStartTime(start);
    b.setEndTime(start.plusHours(2));
    b.setReason("League night");
    return b;
  }
}
