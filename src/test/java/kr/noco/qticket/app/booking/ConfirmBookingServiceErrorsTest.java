package kr.noco.qticket.app.booking;

import static kr.noco.qticket.app.booking.BookingTestSupport.KEY;
import static kr.noco.qticket.app.booking.BookingTestSupport.HOLDER;
import static kr.noco.qticket.app.booking.BookingTestSupport.PERFORMANCE_ID;
import static kr.noco.qticket.app.booking.BookingTestSupport.SEAT_ID;
import static kr.noco.qticket.app.booking.BookingTestSupport.assertErrorCode;
import static kr.noco.qticket.app.booking.BookingTestSupport.claim;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.hold.SeatHoldPort;
import kr.noco.qticket.app.seat.SeatPersistencePort;
import kr.noco.qticket.domain.booking.Booking;
import kr.noco.qticket.domain.booking.BookingClaim;
import kr.noco.qticket.domain.booking.Ticket;
import org.junit.jupiter.api.Test;

/** ConfirmBookingService 오류 계약(CONFLICT·INVALID_INPUT·NOT_FOUND) 검증 (#37 low 정리). */
class ConfirmBookingServiceErrorsTest {

    private final BookingPersistencePort bookings = mock(BookingPersistencePort.class);
    private final SeatPersistencePort seats = mock(SeatPersistencePort.class);
    private final SeatHoldPort holds = mock(SeatHoldPort.class);
    private final ConfirmBookingService service =
            new ConfirmBookingService(bookings, seats, holds);
    private final ConfirmBookingCommand command =
            new ConfirmBookingCommand(PERFORMANCE_ID, SEAT_ID, KEY, HOLDER);

    @Test
    void givenSeatAlreadySold_whenConfirm_thenConflict() {
        given(bookings.claimIdempotencyKey(any())).willReturn(claim(true));
        given(holds.isHeldBy(PERFORMANCE_ID, SEAT_ID, HOLDER)).willReturn(true);
        given(seats.claimForSale(PERFORMANCE_ID, SEAT_ID)).willReturn(false);

        assertErrorCode(ErrorCode.CONFLICT, () -> service.confirm(command));
        verify(bookings, never()).save(any(Ticket.class));
    }

    @Test
    void givenHoldMissing_whenConfirm_thenConflict() {
        given(bookings.claimIdempotencyKey(any())).willReturn(claim(true));
        given(holds.isHeldBy(PERFORMANCE_ID, SEAT_ID, HOLDER)).willReturn(false);

        assertErrorCode(ErrorCode.CONFLICT, () -> service.confirm(command));
        verify(seats, never()).claimForSale(any(), any());
        verify(bookings, never()).save(any(Ticket.class));
    }

    @Test
    void givenSameKeyDifferentSeat_whenConfirm_thenInvalidInput() {
        BookingClaim other = new BookingClaim(
                new Booking(300L, KEY, PERFORMANCE_ID, 999L), true);
        given(bookings.claimIdempotencyKey(any())).willReturn(other);

        assertErrorCode(ErrorCode.INVALID_INPUT, () -> service.confirm(command));
        verify(seats, never()).claimForSale(any(), any());
    }

    @Test
    void givenUnknownSeatForNewClaim_whenConfirm_thenNotFound() {
        given(bookings.claimIdempotencyKey(any()))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND));

        assertErrorCode(ErrorCode.NOT_FOUND, () -> service.confirm(command));
    }
}
