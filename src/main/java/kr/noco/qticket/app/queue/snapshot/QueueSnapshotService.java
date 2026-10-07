package kr.noco.qticket.app.queue.snapshot;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.validation.ExternalIdentifier;
import org.springframework.stereotype.Service;

/** queue 상태 재조회 use case. 권위 값은 Valkey에서 읽는다. */
@Service
public class QueueSnapshotService {

    private final QueueSnapshotReadPort reads;

    public QueueSnapshotService(QueueSnapshotReadPort reads) {
        this.reads = reads;
    }

    public QueueSnapshot snapshot(Long performanceId, String memberId) {
        if (performanceId == null || performanceId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        ExternalIdentifier.requireValid(memberId);
        return reads.read(performanceId, memberId);
    }
}
