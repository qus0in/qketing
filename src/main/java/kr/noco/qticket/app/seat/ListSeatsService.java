package kr.noco.qticket.app.seat;

import java.util.List;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.performance.PerformancePersistencePort;
import kr.noco.qticket.domain.seat.Seat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListSeatsService {

    private final SeatPersistencePort seats;
    private final PerformancePersistencePort performances;

    public ListSeatsService(SeatPersistencePort seats, PerformancePersistencePort performances) {
        this.seats = seats;
        this.performances = performances;
    }

    @Transactional(readOnly = true)
    public List<SeatResult> list(ListSeatsQuery query) {
        requirePerformance(query.performanceId());
        List<Seat> found = seats.findByPerformanceId(query.performanceId(), query.sort(),
                query.direction());
        return found.stream().map(SeatResult::from).toList();
    }

    private void requirePerformance(Long performanceId) {
        performances.findById(performanceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
