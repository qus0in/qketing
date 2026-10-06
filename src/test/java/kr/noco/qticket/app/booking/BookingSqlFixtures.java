package kr.noco.qticket.app.booking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** SQL로 fixture를 직접 넣는 booking 통합 테스트 지원 (#37 low 정리). */
abstract class BookingSqlFixtures {

    @Autowired
    protected JdbcTemplate jdbc;

    protected Long insertPerformance(String title) {
        return jdbc.queryForObject(
                "insert into performance (title, starts_at) values (?, now()) returning id",
                Long.class, title);
    }

    protected Long insertSeat(Long performanceId, String saleStatus) {
        return jdbc.queryForObject(
                "insert into seat (performance_id, section, row_label, seat_number, sale_status)"
                        + " values (?, 'R', '1', 1, ?) returning id",
                Long.class, performanceId, saleStatus);
    }

    protected Integer countBookings(String idempotencyKey) {
        return jdbc.queryForObject(
                "select count(*) from booking where idempotency_key = ?",
                Integer.class, idempotencyKey);
    }

    protected Integer countTickets(Long performanceId) {
        return jdbc.queryForObject(
                "select count(*) from ticket where performance_id = ?",
                Integer.class, performanceId);
    }

    protected String seatSaleStatus(Long seatId) {
        return jdbc.queryForObject("select sale_status from seat where id = ?",
                String.class, seatId);
    }

    protected void cleanupByPerformance(Long performanceId) {
        jdbc.update("delete from ticket where performance_id = ?", performanceId);
        jdbc.update("delete from booking where performance_id = ?", performanceId);
        jdbc.update("delete from seat where performance_id = ?", performanceId);
        jdbc.update("delete from performance where id = ?", performanceId);
    }
}
