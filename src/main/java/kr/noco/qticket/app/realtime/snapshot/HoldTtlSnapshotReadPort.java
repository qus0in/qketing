package kr.noco.qticket.app.realtime.snapshot;

import java.util.List;
import java.util.Map;

/**
 * seat hold 키의 남은 TTL만 조회한다(값·actor ID 미조회).
 * 반환은 seatId -> 잔여 TTL(밀리초). 키 없음·TTL 0 이하인 좌석은 담지 않는다.
 */
public interface HoldTtlSnapshotReadPort {

    Map<Long, Long> holdTtlMillis(Long performanceId, List<Long> seatIds);
}
