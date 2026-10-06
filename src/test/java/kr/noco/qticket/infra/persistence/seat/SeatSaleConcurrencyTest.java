package kr.noco.qticket.infra.persistence.seat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kr.noco.qticket.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** 같은 좌석 동시 판매에서 conditional update가 정확히 한 건만 성공하는지 검증 (#40 T40-13). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SeatSaleConcurrencyTest extends SeatSaleConcurrencyFixture {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void givenSameSeat_whenClaimedConcurrently_thenExactlyOneClaimWins() {
        Long performanceId = createPerformance();
        Long seatId = createSeat(performanceId);
        try {
            List<Outcome> outcomes = claimConcurrently(performanceId, seatId);
            assertOutcomes(outcomes);
            assertSaleStatus(seatId, "SOLD");
        } finally {
            cleanup(performanceId);
        }
    }

    private void assertOutcomes(List<Outcome> outcomes) {
        assertThat(outcomes).hasSize(THREADS).allSatisfy(outcome ->
                assertThat(outcome.failure()).isNull());
        assertThat(outcomes.stream().filter(Outcome::won).count()).isEqualTo(1L);
        assertThat(outcomes.stream().filter(Outcome::lost).count()).isEqualTo(THREADS - 1L);
    }

    private void assertSaleStatus(Long seatId, String expected) {
        assertThat(jdbc.queryForObject("SELECT sale_status FROM seat WHERE id = ?",
                String.class, seatId)).isEqualTo(expected);
    }

    private Long createPerformance() {
        return jdbc.queryForObject("INSERT INTO performance (title, starts_at)"
                + " VALUES (?, now()) RETURNING id", Long.class, "Seat sale concurrency");
    }

    private Long createSeat(Long performanceId) {
        return jdbc.queryForObject("INSERT INTO seat (performance_id, section, row_label,"
                + " seat_number) VALUES (?, 'R', '1', 1) RETURNING id", Long.class,
                performanceId);
    }

    private void cleanup(Long performanceId) {
        jdbc.update("DELETE FROM seat WHERE performance_id = ?", performanceId);
        jdbc.update("DELETE FROM performance WHERE id = ?", performanceId);
    }
}