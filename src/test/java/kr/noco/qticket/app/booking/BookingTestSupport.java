package kr.noco.qticket.app.booking;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.domain.booking.Booking;
import kr.noco.qticket.domain.booking.BookingClaim;
import kr.noco.qticket.domain.booking.Ticket;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/** ConfirmBookingService 단위 테스트 공용 상수·fixture (#37 low 정리). */
final class BookingTestSupport {

    static final Long PERFORMANCE_ID = 7L;
    static final Long SEAT_ID = 42L;
    static final String KEY = "key-1";
    static final String HOLDER = "holder-1";

    private BookingTestSupport() {
    }

    static BookingClaim claim(boolean acquired) {
        return new BookingClaim(new Booking(300L, KEY, PERFORMANCE_ID, SEAT_ID), acquired);
    }

    static Ticket ticket(Long ticketId) {
        return new Ticket(ticketId, 300L, PERFORMANCE_ID, SEAT_ID);
    }

    static void assertErrorCode(ErrorCode expected, ThrowingCallable executable) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(executable)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(expected);
    }
}
