package kr.noco.qticket.app.realtime;

import java.util.Objects;
import kr.noco.qticket.app.validation.ExternalIdentifier;
import kr.noco.qticket.domain.queue.AdmissionStatus;

public record QueueRealtimeEvent(Long performanceId, String memberId, AdmissionStatus status,
                                 long occurredAt) {

    public QueueRealtimeEvent {
        if (performanceId == null || performanceId < 1 || occurredAt < 1) {
            throw new IllegalArgumentException("Queue event identifiers and time must be positive");
        }
        ExternalIdentifier.requireValid(memberId);
        Objects.requireNonNull(status, "status");
    }
}
