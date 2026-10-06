package kr.noco.qticket.app.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.seat.SeatPersistencePort;
import kr.noco.qticket.domain.booking.Booking;
import kr.noco.qticket.domain.booking.BookingClaim;
import kr.noco.qticket.domain.booking.Ticket;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

class ConfirmBookingServiceTest {

    private static final Long PERFORMANCE_ID = 7L;
    private static final Long SEAT_ID = 42L;

    private final BookingPersistencePort bookings = mock(BookingPersistencePort.class);
    private final SeatPersistencePort seats = mock(SeatPersistencePort.class);
    private final ConfirmBookingService service = new ConfirmBookingService(bookings, seats);
    private final ConfirmBookingCommand command = new ConfirmBookingCommand(PERFORMANCE_ID, SEAT_ID, "key-1");

    @Test
    void givenAcquiredClaim_whenConfirm_thenSeatSoldAndTicketSaved() {
        given(bookings.claimIdempotencyKey(any())).willReturn(claim(true));
        given(seats.claimForSale(PERFORMANCE_ID, SEAT_ID)).willReturn(true);
        given(bookings.save(any(Ticket.class))).willReturn(ticket(500L));

        BookingResult result = service.confirm(command);

        assertThat(result).isEqualTo(new BookingResult(300L, 500L, PERFORMANCE_ID, SEAT_ID));
        verify(seats).claimForSale(PERFORMANCE_ID, SEAT_ID);
    }

    @Test
    void givenExistingClaim_whenConfirm_thenReplayWithoutExtraSale() {
        given(bookings.claimIdempotencyKey(any())).willReturn(claim(false));
        given(bookings.findTicketByBookingId(300L)).willReturn(Optional.of(ticket(500L)));

        BookingResult result = service.confirm(command);

        assertThat(result).isEqualTo(new BookingResult(300L, 500L, PERFORMANCE_ID, SEAT_ID));
        verify(seats, never()).claimForSale(any(), any());
        verify(bookings, never()).save(any(Ticket.class));
    }

    @Test
    void givenSeatAlreadySold_whenConfirm_thenConflict() {
        given(bookings.claimIdempotencyKey(any())).willReturn(claim(true));
        given(seats.claimForSale(PERFORMANCE_ID, SEAT_ID)).willReturn(false);
        assertErrorCode(ErrorCode.CONFLICT, () -> service.confirm(command));
        verify(bookings, never()).save(any(Ticket.class));
    }

    @Test
    void givenSameKeyDifferentSeat_whenConfirm_thenInvalidInput() {
        BookingClaim other = new BookingClaim(
                new Booking(300L, "key-1", PERFORMANCE_ID, 999L), true);
        given(bookings.claimIdempotencyKey(any())).willReturn(other);
        assertErrorCode(ErrorCode.INVALID_INPUT, () -> service.confirm(command));
        verify(seats, never()).claimForSale(any(), any());
    }

    @Test
    void givenUnknownSeatForNewClaim_whenConfirm_thenNotFound() {
        given(bookings.claimIdempotencyKey(any())).willThrow(new BusinessException(ErrorCode.NOT_FOUND));
        assertErrorCode(ErrorCode.NOT_FOUND, () -> service.confirm(command));
    }

    @Test
    void givenInvalidCommand_whenCreated_thenInvalidInput() {
        assertErrorCode(ErrorCode.INVALID_INPUT, () -> new ConfirmBookingCommand(0L, SEAT_ID, "key-1"));
        assertErrorCode(ErrorCode.INVALID_INPUT, () -> new ConfirmBookingCommand(PERFORMANCE_ID, SEAT_ID, " "));
        assertErrorCode(ErrorCode.INVALID_INPUT, () -> new ConfirmBookingCommand(PERFORMANCE_ID, SEAT_ID, "k".repeat(129)));
        assertErrorCode(ErrorCode.INVALID_INPUT, () -> service.confirm(null));
    }

    private void assertErrorCode(ErrorCode expected, ThrowingCallable executable) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(executable)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(expected);
    }

    private BookingClaim claim(boolean acquired) {
        return new BookingClaim(new Booking(300L, "key-1", PERFORMANCE_ID, SEAT_ID), acquired);
    }

    private Ticket ticket(Long ticketId) {
        return new Ticket(ticketId, 300L, PERFORMANCE_ID, SEAT_ID);
    }
}
