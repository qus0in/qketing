package kr.noco.qticket.app.queue.snapshot;

public interface QueueSnapshotReadPort {

    QueueSnapshot read(Long performanceId, String memberId);
}
