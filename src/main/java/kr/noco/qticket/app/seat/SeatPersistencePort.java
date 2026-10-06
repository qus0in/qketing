package kr.noco.qticket.app.seat;

import java.util.List;
import kr.noco.qticket.domain.seat.Seat;
import kr.noco.qticket.domain.seat.SeatSort;
import kr.noco.qticket.domain.seat.SortDirection;

public interface SeatPersistencePort {

    List<Seat> findByPerformanceId(Long performanceId, SeatSort sort, SortDirection direction);

    /** Atomically changes AVAILABLE to SOLD; false means another request already claimed it. */
    boolean claimForSale(Long performanceId, Long seatId);
}
