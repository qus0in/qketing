package kr.noco.qticket.app.booking;

import static kr.noco.qticket.app.booking.BookingTestSupport.PERFORMANCE_ID;
import static kr.noco.qticket.app.booking.BookingTestSupport.HOLDER;
import static kr.noco.qticket.app.booking.BookingTestSupport.SEAT_ID;
import static kr.noco.qticket.app.booking.BookingTestSupport.assertErrorCode;
import static org.mockito.Mockito.mock;

import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.hold.SeatHoldPort;
import kr.noco.qticket.app.seat.SeatPersistencePort;
import org.junit.jupiter.api.Test;

class ConfirmBookingCommandTest {

    private final BookingPersistencePort bookings = mock(BookingPersistencePort.class);
    private final SeatPersistencePort seats = mock(SeatPersistencePort.class);
    private final SeatHoldPort holds = mock(SeatHoldPort.class);
    private final BookingCommitEffects commitEffects = mock(BookingCommitEffects.class);
    private final ConfirmBookingService service =
            new ConfirmBookingService(bookings, seats, holds, commitEffects);

    @Test
    void givenInvalidCommand_whenCreated_thenInvalidInput() {
        assertErrorCode(ErrorCode.INVALID_INPUT,
                () -> new ConfirmBookingCommand(0L, SEAT_ID, "key-1", HOLDER));
        assertErrorCode(ErrorCode.INVALID_INPUT,
                () -> new ConfirmBookingCommand(PERFORMANCE_ID, SEAT_ID, " ", HOLDER));
        assertErrorCode(ErrorCode.INVALID_INPUT,
                () -> new ConfirmBookingCommand(PERFORMANCE_ID, SEAT_ID, "k".repeat(129),
                        HOLDER));
        assertErrorCode(ErrorCode.INVALID_INPUT,
                () -> new ConfirmBookingCommand(PERFORMANCE_ID, SEAT_ID, "key-1", " "));
        assertErrorCode(ErrorCode.INVALID_INPUT,
                () -> new ConfirmBookingCommand(PERFORMANCE_ID, SEAT_ID, "key-1",
                        "h".repeat(129)));
        assertErrorCode(ErrorCode.INVALID_INPUT,
                () -> new ConfirmBookingCommand(PERFORMANCE_ID, SEAT_ID, "key-1", "holder id!"));
        assertErrorCode(ErrorCode.INVALID_INPUT, () -> service.confirm(null));
    }
}
