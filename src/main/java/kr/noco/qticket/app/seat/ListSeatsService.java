package kr.noco.qticket.app.seat;

import java.util.List;
import kr.noco.qticket.domain.seat.Seat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListSeatsService {

    private final SeatPersistencePort seats;

    public ListSeatsService(SeatPersistencePort seats) {
        this.seats = seats;
    }

    @Transactional(readOnly = true)
    public List<SeatResult> list(ListSeatsQuery query) {
        List<Seat> found = seats.findByPerformanceId(query.performanceId(), query.sort(),
                query.direction());
        return found.stream().map(SeatResult::from).toList();
    }
}
