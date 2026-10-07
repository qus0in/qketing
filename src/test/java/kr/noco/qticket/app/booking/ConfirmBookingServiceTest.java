package kr.noco.qticket.app.booking;

import static kr.noco.qticket.app.booking.BookingTestSupport.KEY;
import static kr.noco.qticket.app.booking.BookingTestSupport.HOLDER;
import static kr.noco.qticket.app.booking.BookingTestSupport.PERFORMANCE_ID;
import static kr.noco.qticket.app.booking.BookingTestSupport.SEAT_ID;
import static kr.noco.qticket.app.booking.BookingTestSupport.claim;
import static kr.noco.qticket.app.booking.BookingTestSupport.ticket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import kr.noco.qticket.app.hold.SeatHoldPort;
import kr.noco.qticket.app.seat.SeatPersistencePort;
import kr.noco.qticket.domain.booking.Ticket;
import org.junit.jupiter.api.Test;

/** ConfirmBookingService 정상 흐름(신규 판매·replay) 검증 (#37 low 정리). */
class ConfirmBookingServiceTest {

    private final BookingPersistencePort bookings = mock(BookingPersistencePort.class);
    private final SeatPersistencePort seats = mock(SeatPersistencePort.class);
    private final SeatHoldPort holds = mock(SeatHoldPort.class);
    private final BookingCommitEffects commitEffects = mock(BookingCommitEffects.class);
    private final ConfirmBookingService service =
            new ConfirmBookingService(bookings, seats, holds, commitEffects);
    private final ConfirmBookingCommand command =
            new ConfirmBookingCommand(PERFORMANCE_ID, SEAT_ID, KEY, HOLDER);

    @Test
    void givenAcquiredClaim_whenConfirm_thenSeatSoldAndTicketSaved() {
        given(bookings.claimIdempotencyKey(any())).willReturn(claim(true));
        given(holds.isHeldBy(PERFORMANCE_ID, SEAT_ID, HOLDER)).willReturn(true);
        given(seats.claimForSale(PERFORMANCE_ID, SEAT_ID)).willReturn(true);
        given(bookings.save(any(Ticket.class))).willReturn(ticket(500L));

        BookingResult result = service.confirm(command);

        assertThat(result).isEqualTo(new BookingResult(300L, 500L, PERFORMANCE_ID, SEAT_ID));
        verify(seats).claimForSale(PERFORMANCE_ID, SEAT_ID);
        verify(commitEffects).releaseHoldAndPublishSold(PERFORMANCE_ID, SEAT_ID, HOLDER);
    }

    @Test
    void givenExistingClaim_whenConfirm_thenReplayWithoutExtraSale() {
        given(bookings.claimIdempotencyKey(any())).willReturn(claim(false));
        given(bookings.findTicketByBookingId(300L)).willReturn(Optional.of(ticket(500L)));

        BookingResult result = service.confirm(command);

        assertThat(result).isEqualTo(new BookingResult(300L, 500L, PERFORMANCE_ID, SEAT_ID));
        verify(holds, never()).isHeldBy(any(), any(), any());
        verify(commitEffects, never()).releaseHoldAndPublishSold(any(), any(), any());
        verify(seats, never()).claimForSale(any(), any());
        verify(bookings, never()).save(any(Ticket.class));
    }
}
