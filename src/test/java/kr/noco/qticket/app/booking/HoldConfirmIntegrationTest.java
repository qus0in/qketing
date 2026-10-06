package kr.noco.qticket.app.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** hold 소유 확인과 commit 후 해제 계약 검증 (#40 T40-10). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class HoldConfirmIntegrationTest extends BookingIntegrationSupport {

    @Test
    void givenNoHold_whenConfirm_thenConflictWithoutPersisting() {
        Fixture fixture = createFixture();
        try {
            assertThatThrownBy(() -> confirm(fixture, fixture.firstSeatId(),
                    UUID.randomUUID().toString(), "nobody-" + UUID.randomUUID()))
                    .isInstanceOfSatisfying(BusinessException.class,
                            error -> assertThat(error.getErrorCode())
                                    .isEqualTo(ErrorCode.CONFLICT));
            assertCounts(fixture, 0L, 0L);
        } finally {
            cleanup(fixture);
        }
    }

    @Test
    void givenOwnedHold_whenConfirm_thenHoldReleasedAfterCommit() {
        Fixture fixture = createFixture();
        String owner = "owner-" + UUID.randomUUID();
        assertThat(holds.hold(fixture.performanceId(), fixture.firstSeatId(), owner))
                .isTrue();
        try {
            confirm(fixture, fixture.firstSeatId(), UUID.randomUUID().toString(), owner);
            assertThat(holds.isHeldBy(fixture.performanceId(), fixture.firstSeatId(), owner))
                    .isFalse();
        } finally {
            cleanup(fixture);
        }
    }
}
