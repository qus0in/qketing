package kr.noco.qticket.app.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.infra.persistence.seat.SeatEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** 서로 다른 key로 같은 좌석을 동시 confirm할 때 1건만 판매되는지 검증 (#33). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrentBookingConfirmTest extends BookingConcurrencySupport {

    @Test
    void givenDifferentKeys_whenConfirmingSameSeatConcurrently_thenOnlyOneTicketIsSold() {
        Fixture fixture = createFixture();
        String owner = "owner-" + UUID.randomUUID();
        assertThat(holds.hold(fixture.performanceId(), fixture.firstSeatId(), owner))
                .isTrue();
        try {
            List<Attempt> attempts = executeConcurrently(fixture,
                    () -> UUID.randomUUID().toString(), holders(owner));
            assertOutcomes(attempts, fixture.firstSeatId());
            assertPersistedResult(fixture);
        } finally {
            cleanup(fixture);
        }
    }

    private Supplier<String> holders(String owner) {
        AtomicBoolean first = new AtomicBoolean(true);
        return () -> first.getAndSet(false) ? owner : "other-" + UUID.randomUUID();
    }

    private void assertOutcomes(List<Attempt> attempts, Long seatId) {
        assertThat(attempts).hasSize(WORKERS).allSatisfy(attempt ->
                assertThat(attempt.failure()).isNull());
        assertThat(attempts).filteredOn(attempt -> attempt.error() == null).singleElement()
                .satisfies(winner -> assertThat(winner.result().seatId()).isEqualTo(seatId));
        assertThat(attempts).filteredOn(attempt -> attempt.error() == ErrorCode.CONFLICT)
                .hasSize(WORKERS - 1);
    }

    private void assertPersistedResult(Fixture fixture) {
        assertCounts(fixture, 1L, 1L);
        assertThat(seats.findById(fixture.firstSeatId()).orElseThrow().getSaleStatus())
                .isEqualTo(SeatEntity.SaleStatus.SOLD);
    }
}
