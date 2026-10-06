package kr.noco.qticket.app.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** SOLD 좌석 CONFLICT 시 proxied app bean transaction이 전부 rollback되는지 검증 (#33 조각 13). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConfirmBookingRollbackTest {

    @Autowired
    private ConfirmBookingService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void givenSoldSeat_whenConfirmRequested_thenConflictAndNoBookingOrTicketRemains() {
        Long performanceId = insertPerformance();
        Long seatId = insertSeat(performanceId, "SOLD");

        assertThatThrownBy(() -> service.confirm(
                new ConfirmBookingCommand(performanceId, seatId, "rollback-key-1")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

        assertThat(countBookings("rollback-key-1")).isZero();
        assertThat(countTickets(performanceId)).isZero();
        assertThat(seatSaleStatus(seatId)).isEqualTo("SOLD");
        cleanup(performanceId);
    }

    private Long insertPerformance() {
        return jdbcTemplate.queryForObject(
                "insert into performance (title, starts_at) values (?, now()) returning id",
                Long.class, "롤백 검증 공연");
    }

    private Long insertSeat(Long performanceId, String saleStatus) {
        return jdbcTemplate.queryForObject(
                "insert into seat (performance_id, section, row_label, seat_number, sale_status)"
                        + " values (?, 'R', '1', 1, ?) returning id",
                Long.class, performanceId, saleStatus);
    }

    private Integer countBookings(String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "select count(*) from booking where idempotency_key = ?",
                Integer.class, idempotencyKey);
    }

    private Integer countTickets(Long performanceId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from ticket where performance_id = ?",
                Integer.class, performanceId);
    }

    private String seatSaleStatus(Long seatId) {
        return jdbcTemplate.queryForObject("select sale_status from seat where id = ?",
                String.class, seatId);
    }

    private void cleanup(Long performanceId) {
        jdbcTemplate.update("delete from ticket where performance_id = ?", performanceId);
        jdbcTemplate.update("delete from booking where performance_id = ?", performanceId);
        jdbcTemplate.update("delete from seat where performance_id = ?", performanceId);
        jdbcTemplate.update("delete from performance where id = ?", performanceId);
    }
}
