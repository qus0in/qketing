package kr.noco.qticket.app.realtime.snapshot;

import java.util.List;

/** 성능(공연) 단위 좌석 sale_status 원본 조회. infra/persistence/seat 구현체가 구현한다. */
public interface SeatSaleSnapshotReadPort {

    List<SeatSaleState> findByPerformanceId(Long performanceId);
}
