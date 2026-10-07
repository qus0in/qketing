package kr.noco.qticket.app.realtime.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;

/** performanceId 시점의 좌석 원본 상태 스냅샷. seats는 불변 리스트다. */
public record SeatSnapshot(Long performanceId, List<SeatSnapshotEntry> seats,
                           Instant capturedAt) {

    public SeatSnapshot {
        if (performanceId == null || performanceId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (capturedAt == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        seats = List.copyOf(Objects.requireNonNull(seats));
    }
}
