package kr.noco.qticket.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.infra.persistence.performance.PerformanceEntity;
import kr.noco.qticket.infra.persistence.performance.PerformanceJpaRepository;
import kr.noco.qticket.infra.persistence.seat.SeatEntity;
import kr.noco.qticket.infra.persistence.seat.SeatJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** PostgreSQL(Testcontainers) 영속 smoke (#27). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PersistenceSmokeTest {

    @Autowired
    private PerformanceJpaRepository performanceRepository;

    @Autowired
    private SeatJpaRepository seatRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void givenPerformanceWithTwoSeats_whenSaved_thenFindable() {
        PerformanceEntity saved = performanceRepository.saveAndFlush(
                PerformanceEntity.of("오페라의 유령", Instant.parse("2026-11-01T10:00:00Z")));
        seatRepository.saveAndFlush(SeatEntity.of(saved.getId(), "A", "1", 1));
        seatRepository.saveAndFlush(SeatEntity.of(saved.getId(), "A", "1", 2));

        assertThat(performanceRepository.findAll()).hasSize(1);
        assertThat(seatRepository.findAll()).hasSize(2);
        assertThat(seatRepository.findAll()).allSatisfy(seat ->
                assertThat(seat.getPerformanceId()).isEqualTo(saved.getId()));
    }

    @Test
    void givenSameSeatPosition_whenSavedTwice_thenDataIntegrityViolation() {
        PerformanceEntity saved = performanceRepository.saveAndFlush(
                PerformanceEntity.of("겨울 나그네", Instant.parse("2026-11-02T10:00:00Z")));
        Long performanceId = saved.getId();
        seatRepository.saveAndFlush(SeatEntity.of(performanceId, "B", "3", 7));

        assertThatThrownBy(() -> seatRepository.saveAndFlush(
                SeatEntity.of(performanceId, "B", "3", 7)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenFlywayV1Applied_whenQueried_thenVectorExtensionExists() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from pg_extension where extname = 'vector'", Integer.class);

        assertThat(count).isEqualTo(1);
    }
}
