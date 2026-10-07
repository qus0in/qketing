package kr.noco.qticket.realtime.support;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** insert returning id로 고유 공연·좌석 행을 만드는 SQL fixture. 실행할 때마다 새 공연을 쓴다. */
public class RealtimeFixtureSql {

    private final JdbcTemplate jdbc;

    public RealtimeFixtureSql(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SeatFixture seat() {
        String title = "rt-" + UUID.randomUUID();
        Long performanceId = jdbc.queryForObject(
                "insert into performance (title, starts_at) values (?, now()) returning id",
                Long.class, title);
        Long seatId = jdbc.queryForObject(
                "insert into seat (performance_id, section, row_label, seat_number, sale_status)"
                        + " values (?, 'R', '1', 1, 'AVAILABLE') returning id",
                Long.class, performanceId);
        return new SeatFixture(performanceId, seatId);
    }

    public void markSold(Long seatId) {
        jdbc.update("update seat set sale_status = 'SOLD' where id = ?", seatId);
    }

    public String holdKey(Long performanceId, Long seatId) {
        return "qticket:hold:{" + performanceId + "}:" + seatId;
    }

    public record SeatFixture(Long performanceId, Long seatId) {
    }
}
