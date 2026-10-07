package kr.noco.qticket.ui.realtime.api;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshot;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshotService;
import kr.noco.qticket.app.validation.ExternalIdentifier;
import kr.noco.qticket.ui.realtime.sse.SseConnectionManager;
import kr.noco.qticket.ui.realtime.sse.SseEvent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** queue SSE 스트림. source snapshot 실패를 숨기지 않고 caller가 ProblemDetail로 관측한다. */
@RestController
public class QueueStreamController {

    private final QueueSnapshotService snapshots;
    private final SseConnectionManager connections;
    private final QueueStreamProperties properties;

    public QueueStreamController(QueueSnapshotService snapshots, SseConnectionManager connections,
                                 QueueStreamProperties properties) {
        this.snapshots = snapshots;
        this.connections = connections;
        this.properties = properties;
    }

    @GetMapping(value = "/api/performances/{performanceId}/queue/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long performanceId, @RequestParam String memberId) {
        requirePositive(performanceId);
        ExternalIdentifier.requireValid(memberId);
        SseConnectionManager.SubscriberKey key =
                new SseConnectionManager.SubscriberKey(performanceId, memberId);
        SseEmitter emitter = new SseEmitter(properties.timeout().toMillis());
        connections.registerWithSnapshot(key, emitter, () -> snapshot(performanceId, memberId));
        return emitter;
    }

    private SseEvent snapshot(Long performanceId, String memberId) {
        QueueSnapshot snapshot = snapshots.snapshot(performanceId, memberId);
        return SseEvent.of("queue", snapshot, snapshot.capturedAt().toEpochMilli());
    }

    private void requirePositive(Long performanceId) {
        if (performanceId == null || performanceId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
