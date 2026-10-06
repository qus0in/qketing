package kr.noco.qticket.app.seat;

import java.time.Instant;
import kr.noco.qticket.domain.seat.Seat;

public record SeatResult(Long id, Long performanceId, String section, String rowLabel,
                         Integer seatNumber, Instant createdAt) {

    public static SeatResult from(Seat seat) {
        return new SeatResult(seat.id(), seat.performanceId(), seat.section(), seat.rowLabel(),
                seat.seatNumber(), seat.createdAt());
    }
}
