package kr.noco.qticket.ui.realtime.api;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshot;
import kr.noco.qticket.app.realtime.snapshot.SeatSnapshotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 좌석 snapshot REST API. 권위 값은 worker1의 SeatSnapshotService가 조립한다. */
@RestController
public class SeatSnapshotController {

    private final SeatSnapshotService snapshots;

    public SeatSnapshotController(SeatSnapshotService snapshots) {
        this.snapshots = snapshots;
    }

    @GetMapping("/api/performances/{performanceId}/seats/snapshot")
    public SeatSnapshot snapshot(@PathVariable Long performanceId) {
        if (performanceId == null || performanceId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return snapshots.snapshot(performanceId);
    }
}
