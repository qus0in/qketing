package kr.noco.qticket.app.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

/** idempotent replay 테스트 공용 hold·결과 검증 헬퍼 (#37 low 정리). */
abstract class BookingReplaySupport extends BookingConcurrencySupport {

    protected String holdFirstSeat(Fixture fixture) {
        String holder = "holder-" + UUID.randomUUID();
        assertThat(holds.hold(fixture.performanceId(), fixture.firstSeatId(), holder))
                .isTrue();
        return holder;
    }

    protected void assertAllSucceeded(List<Attempt> attempts) {
        assertThat(attempts).hasSize(WORKERS)
                .allMatch(attempt -> attempt.failure() == null && attempt.error() == null);
        assertThat(attempts).extracting(Attempt::result)
                .containsOnly(attempts.get(0).result());
    }
}
