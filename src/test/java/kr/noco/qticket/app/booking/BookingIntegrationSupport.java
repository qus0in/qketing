package kr.noco.qticket.app.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import kr.noco.qticket.app.hold.SeatHoldPort;
import kr.noco.qticket.infra.persistence.performance.PerformanceEntity;
import kr.noco.qticket.infra.persistence.performance.PerformanceJpaRepository;
import kr.noco.qticket.infra.persistence.seat.SeatEntity;
import kr.noco.qticket.infra.persistence.seat.SeatJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** booking confirm 통합 테스트 공용 fixture (#37 low 정리). */
abstract class BookingIntegrationSupport {

    @Autowired
    protected ConfirmBookingService confirmations;

    @Autowired
    protected PerformanceJpaRepository performances;

    @Autowired
    protected SeatJpaRepository seats;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected SeatHoldPort holds;

    protected Fixture createFixture() {
        PerformanceEntity performance = performances.saveAndFlush(
                PerformanceEntity.of("Booking integration show", Instant.now()));
        SeatEntity first = seats.saveAndFlush(SeatEntity.of(performance.getId(), "A", "A", 1));
        SeatEntity second = seats.saveAndFlush(SeatEntity.of(performance.getId(), "A", "A", 2));
        return new Fixture(performance.getId(), first.getId(), second.getId());
    }

    protected BookingResult confirm(Fixture fixture, Long seatId, String key, String holder) {
        return confirmations.confirm(
                new ConfirmBookingCommand(fixture.performanceId(), seatId, key, holder));
    }

    protected void assertCounts(Fixture fixture, Long bookings, Long tickets) {
        assertThat(count("SELECT count(*) FROM booking WHERE performance_id = ?",
                fixture.performanceId())).isEqualTo(bookings);
        assertThat(count("SELECT count(*) FROM ticket WHERE performance_id = ?",
                fixture.performanceId())).isEqualTo(tickets);
    }

    protected void cleanup(Fixture fixture) {
        jdbc.update("DELETE FROM ticket WHERE performance_id = ?", fixture.performanceId());
        jdbc.update("DELETE FROM booking WHERE performance_id = ?", fixture.performanceId());
        jdbc.update("DELETE FROM seat WHERE performance_id = ?", fixture.performanceId());
        jdbc.update("DELETE FROM performance WHERE id = ?", fixture.performanceId());
    }

    private Long count(String sql, Long performanceId) {
        return jdbc.queryForObject(sql, Long.class, performanceId);
    }

    protected record Fixture(Long performanceId, Long firstSeatId, Long secondSeatId) {}
}
