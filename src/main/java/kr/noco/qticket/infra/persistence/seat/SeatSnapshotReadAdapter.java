package kr.noco.qticket.infra.persistence.seat;

import java.util.List;
import kr.noco.qticket.app.realtime.snapshot.SeatSaleSnapshotReadPort;
import kr.noco.qticket.app.realtime.snapshot.SeatSaleState;
import org.springframework.stereotype.Repository;

/** PostgreSQL의 실제 sale_status를 조회해 snapshot port 계약에 매핑한다. */
@Repository
public class SeatSnapshotReadAdapter implements SeatSaleSnapshotReadPort {

    private final SeatSnapshotJpaRepository repository;

    public SeatSnapshotReadAdapter(SeatSnapshotJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SeatSaleState> findByPerformanceId(Long performanceId) {
        return repository.findSeatsByPerformanceId(performanceId).stream()
                .map(this::toState)
                .toList();
    }

    private SeatSaleState toState(SeatEntity seat) {
        return new SeatSaleState(seat.getId(), seat.getSaleStatus().name());
    }
}
