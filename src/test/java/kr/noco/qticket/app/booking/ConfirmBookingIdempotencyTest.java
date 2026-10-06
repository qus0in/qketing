package kr.noco.qticket.app.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.infra.persistence.seat.SeatEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** 같은 idempotency key의 replay 계약 검증 (#33). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConfirmBookingIdempotencyTest extends BookingReplaySupport {

    @Test
    void givenSameKeyAndInput_whenRetriedSequentially_thenReturnsSameIds() {
        Fixture fixture = createFixture();
        String holder = holdFirstSeat(fixture);
        try {
            String key = UUID.randomUUID().toString();
            BookingResult first = confirm(fixture, fixture.firstSeatId(), key, holder);
            BookingResult second = confirm(fixture, fixture.firstSeatId(), key, holder);
            assertThat(second).isEqualTo(first);
            assertCounts(fixture, 1L, 1L);
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void givenSameKeyAndInput_whenRetriedConcurrently_thenAllResultsMatch() {
        Fixture fixture = createFixture();
        String holder = holdFirstSeat(fixture);
        try {
            assertConcurrentReplay(fixture, holder);
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void givenSameKeyWithDifferentSeat_whenRetried_thenRejectsWithoutChangingBooking() {
        Fixture fixture = createFixture();
        String holder = holdFirstSeat(fixture);
        try {
            String key = UUID.randomUUID().toString();
            BookingResult original = confirm(fixture, fixture.firstSeatId(), key, holder);
            assertThatThrownBy(() -> confirm(fixture, fixture.secondSeatId(), key, holder))
                    .isInstanceOf(BusinessException.class)
                    .extracting(exception -> ((BusinessException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT);
            assertReplayAfterMismatch(fixture, key, holder, original);
        } finally {
            cleanup(fixture);
        }
    }

    private void assertConcurrentReplay(Fixture fixture, String holder) {
        String key = UUID.randomUUID().toString();
        List<Attempt> attempts = executeConcurrently(fixture, () -> key, () -> holder);
        assertAllSucceeded(attempts);
        assertCounts(fixture, 1L, 1L);
        assertThat(seats.findById(fixture.firstSeatId()).orElseThrow().getSaleStatus())
                .isEqualTo(SeatEntity.SaleStatus.SOLD);
    }

    private void assertReplayAfterMismatch(Fixture fixture, String key, String holder,
            BookingResult original) {
        assertThat(confirm(fixture, fixture.firstSeatId(), key, holder)).isEqualTo(original);
        assertCounts(fixture, 1L, 1L);
        assertThat(seats.findById(fixture.secondSeatId()).orElseThrow().getSaleStatus())
                .isEqualTo(SeatEntity.SaleStatus.AVAILABLE);
    }
}
