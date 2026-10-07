package kr.noco.qticket.ui.realtime.api;

import kr.noco.qticket.app.queue.snapshot.QueueSnapshot;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshotService;
import kr.noco.qticket.app.realtime.QueueRealtimeEvent;
import kr.noco.qticket.ui.realtime.sse.SseConnectionManager;
import kr.noco.qticket.ui.realtime.sse.SseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** queue Pub/Sub 이벤트를 받아 해당 subscriber의 원본 snapshot을 재조회해 그 키로만 SSE 전달한다. */
@Component
public class QueueRealtimeEventBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueRealtimeEventBridge.class);

    private final QueueSnapshotService snapshots;
    private final SseConnectionManager connections;

    public QueueRealtimeEventBridge(QueueSnapshotService snapshots,
                                    SseConnectionManager connections) {
        this.snapshots = snapshots;
        this.connections = connections;
    }

    @EventListener
    public void onQueueRealtimeEvent(QueueRealtimeEvent event) {
        SseConnectionManager.SubscriberKey key =
                new SseConnectionManager.SubscriberKey(event.performanceId(), event.memberId());
        try {
            QueueSnapshot snapshot = snapshots.snapshot(event.performanceId(), event.memberId());
            connections.emit(key, SseEvent.of("queue", snapshot, snapshot.capturedAt().toEpochMilli()));
        } catch (RuntimeException exception) {
            LOGGER.warn("Queue realtime bridge failed performanceId={}", event.performanceId(),
                    exception);
        }
    }
}
