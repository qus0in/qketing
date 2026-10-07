package kr.noco.qticket.app.queue.snapshot;

import java.time.Instant;
import java.util.Objects;

/** subscriber별 queue 상태 snapshot. actor 식별자(memberId)는 담지 않는다. */
public record QueueSnapshot(Long performanceId, Status status, int position, long sessionTtlMillis,
                            Instant capturedAt) {

    public QueueSnapshot {
        Objects.requireNonNull(performanceId, "performanceId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(capturedAt, "capturedAt");
    }

    public enum Status {
        ACTIVE,
        WAITING,
        ABSENT
    }
}
