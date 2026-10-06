package kr.noco.qticket.infra.persistence.seat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.infra.persistence.PersistenceConfiguration;
import kr.noco.qticket.infra.persistence.performance.PerformanceEntity;
import kr.noco.qticket.infra.persistence.performance.PerformanceJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, PersistenceConfiguration.class,
        SeatQueryRepository.class})
class SeatQueryRepositoryTest {

    @Autowired
    private SeatQueryRepository seats;

    @Autowired
    private SeatJpaRepository seatJpaRepository;

    @Autowired
    private PerformanceJpaRepository performanceJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void GivenMixedSeatsAndAnotherPerformance_WhenSortedByPositionAscending_ThenFiltersAndOrdersByPosition() {
        Long performanceId = createPerformance();
        Long otherPerformanceId = createPerformance();
        saveSeat(otherPerformanceId, "A", "A", 4);
        saveSeat(performanceId, "A", "A", 3);
        saveSeat(performanceId, "A", "A", 1);
        saveSeat(performanceId, "A", "A", 2);
        saveSeat(performanceId, "A", "B", 1);
        saveSeat(performanceId, "B", "A", 1);

        List<SeatEntity> result = seats.findByPerformanceId(performanceId,
                SeatSort.POSITION, SortDirection.ASC);

        assertThat(result).extracting(SeatEntity::getSection, SeatEntity::getRowLabel,
                        SeatEntity::getSeatNumber)
                .containsExactly(tuple("A", "A", 1), tuple("A", "A", 2), tuple("A", "A", 3),
                        tuple("A", "B", 1), tuple("B", "A", 1));
    }

    @Test
    void GivenSeatCreationTimes_WhenSortedByCreatedAtDescending_ThenReturnsNewestFirst() {
        Long performanceId = createPerformance();
        SeatEntity oldest = saveSeat(performanceId, "A", "A", 1);
        SeatEntity newest = saveSeat(performanceId, "A", "A", 2);
        setCreatedAt(oldest.getId(), "2026-01-01T00:00:00Z");
        setCreatedAt(newest.getId(), "2026-01-02T00:00:00Z");

        List<SeatEntity> result = seats.findByPerformanceId(performanceId,
                SeatSort.CREATED_AT, SortDirection.DESC);

        assertThat(result).extracting(SeatEntity::getId)
                .containsExactly(newest.getId(), oldest.getId());
    }

    @Test
    void GivenNullSortOrDirection_WhenListingSeats_ThenRejectsInput() {
        assertThatThrownBy(() -> seats.findByPerformanceId(1L, null, SortDirection.ASC))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> seats.findByPerformanceId(1L, SeatSort.POSITION, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private Long createPerformance() {
        PerformanceEntity performance = PerformanceEntity.of("Show", Instant.now());
        return performanceJpaRepository.saveAndFlush(performance).getId();
    }

    private SeatEntity saveSeat(Long performanceId, String section, String row, Integer number) {
        return seatJpaRepository.saveAndFlush(SeatEntity.of(performanceId, section, row, number));
    }

    private void setCreatedAt(Long id, String timestamp) {
        jdbcTemplate.update("UPDATE seat SET created_at = ? WHERE id = ?",
                OffsetDateTime.parse(timestamp), id);
    }
}
