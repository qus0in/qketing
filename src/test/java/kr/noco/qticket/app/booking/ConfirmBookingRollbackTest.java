package kr.noco.qticket.app.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.hold.SeatHoldPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** SOLD 좌석 CONFLICT 시 proxied app bean transaction이 전부 rollback되는지 검증 (#33 조각 13). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConfirmBookingRollbackTest extends BookingSqlFixtures {

    @Autowired
    private ConfirmBookingService service;

    @Autowired
    private SeatHoldPort holds;

    @Test
    void givenSoldSeat_whenConfirmRequested_thenConflictAndNoBookingOrTicketRemains() {
        Long performanceId = insertPerformance("롤백 검증 공연");
        Long seatId = insertSeat(performanceId, "SOLD");
        String owner = "rollback-owner";
        assertThat(holds.hold(performanceId, seatId, owner)).isTrue();
        try {
            assertConflictWithoutPersistence(performanceId, seatId, owner);
        } finally {
            holds.release(performanceId, seatId, owner);
            cleanupByPerformance(performanceId);
        }
    }

    private void assertConflictWithoutPersistence(Long performanceId, Long seatId,
            String owner) {
        assertThatThrownBy(() -> service.confirm(
                new ConfirmBookingCommand(performanceId, seatId, "rollback-key-1", owner)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        assertThat(countBookings("rollback-key-1")).isZero();
        assertThat(countTickets(performanceId)).isZero();
        assertThat(seatSaleStatus(seatId)).isEqualTo("SOLD");
        assertThat(holds.isHeldBy(performanceId, seatId, owner)).isTrue();
    }
}
