package kr.noco.qticket.app.realtime.snapshot;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * authoritative seat snapshot: PostgreSQL sale_status 원본과 Valkey hold TTL을 병합한다.
 * Valkey는 source of truth가 아니므로 판매 상태는 항상 DB 값을 따른다(SOLD 우선).
 */
@Service
public class SeatSnapshotService {

    private final SeatSaleSnapshotReadPort sales;
    private final HoldTtlSnapshotReadPort holds;

    public SeatSnapshotService(SeatSaleSnapshotReadPort sales, HoldTtlSnapshotReadPort holds) {
        this.sales = sales;
        this.holds = holds;
    }

    @Transactional(readOnly = true)
    public SeatSnapshot snapshot(Long performanceId) {
        requirePositive(performanceId);
        List<SeatSaleState> states = sales.findByPerformanceId(performanceId);
        Map<Long, Long> ttls = holds.holdTtlMillis(performanceId, sortedIds(states));
        List<SeatSnapshotEntry> entries = states.stream()
                .sorted(Comparator.comparing(SeatSaleState::seatId))
                .map(state -> merge(state, ttls))
                .toList();
        return new SeatSnapshot(performanceId, entries, Instant.now());
    }

    private SeatSnapshotEntry merge(SeatSaleState state, Map<Long, Long> ttls) {
        if (SeatSnapshotEntry.SOLD.equals(state.saleStatus())) {
            return new SeatSnapshotEntry(state.seatId(), state.saleStatus(), false, 0L);
        }
        long ttl = Math.max(0L, ttls.getOrDefault(state.seatId(), 0L));
        return new SeatSnapshotEntry(state.seatId(), state.saleStatus(), ttl > 0, ttl);
    }

    private List<Long> sortedIds(List<SeatSaleState> states) {
        return states.stream().map(SeatSaleState::seatId).sorted().toList();
    }

    private void requirePositive(Long performanceId) {
        if (performanceId == null || performanceId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
