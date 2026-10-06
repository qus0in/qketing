package kr.noco.qticket.infra.persistence.booking;

import java.time.Instant;
import kr.noco.qticket.infra.persistence.performance.PerformanceEntity;
import kr.noco.qticket.infra.persistence.performance.PerformanceJpaRepository;
import kr.noco.qticket.infra.persistence.seat.SeatEntity;
import kr.noco.qticket.infra.persistence.seat.SeatJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** booking/ticket 영속 제약 테스트 공용 fixture (#37 low 정리). */
abstract class BookingPersistenceSupport {

    @Autowired
    protected BookingJpaRepository bookingRepository;

    @Autowired
    protected TicketJpaRepository ticketRepository;

    @Autowired
    protected PerformanceJpaRepository performanceRepository;

    @Autowired
    protected SeatJpaRepository seatRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected PerformanceEntity createPerformance() {
        return performanceRepository.saveAndFlush(
                PerformanceEntity.of("공연", Instant.parse("2026-11-01T10:00:00Z")));
    }

    protected SeatEntity createSeat(Long performanceId, int seatNumber) {
        return seatRepository.saveAndFlush(
                SeatEntity.of(performanceId, "A", "A", seatNumber));
    }
}
