package kr.noco.qticket.ui.realtime.api;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshot;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshotService;
import kr.noco.qticket.ui.realtime.sse.SseConnectionManager;
import kr.noco.qticket.ui.realtime.sse.SseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 연결된 subscriber에게 heartbeat를 보내고 queue snapshot을 주기 재조회해 상태를 갱신한다. */
@Component
public class QueueStreamBroadcaster {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueStreamBroadcaster.class);

    private final SseConnectionManager connections;
    private final QueueSnapshotService snapshots;
    private final QueueStreamProperties properties;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "queue-stream-broadcaster");
                thread.setDaemon(true);
                return thread;
            });

    public QueueStreamBroadcaster(SseConnectionManager connections, QueueSnapshotService snapshots,
                                  QueueStreamProperties properties) {
        this.connections = connections;
        this.snapshots = snapshots;
        this.properties = properties;
    }

    @PostConstruct
    void start() {
        long heartbeat = properties.heartbeatInterval().toMillis();
        long refresh = properties.refreshInterval().toMillis();
        scheduler.scheduleWithFixedDelay(() -> guard(this::heartbeat), heartbeat, heartbeat,
                TimeUnit.MILLISECONDS);
        scheduler.scheduleWithFixedDelay(() -> guard(this::refresh), refresh, refresh,
                TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
    }

    private void guard(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException exception) {
            LOGGER.warn("Queue stream broadcast task failed", exception);
        }
    }

    private void heartbeat() {
        for (SseConnectionManager.SubscriberKey key : connections.subscriberKeys()) {
            connections.emit(key, SseEvent.of("heartbeat", ""));
        }
    }

    private void refresh() {
        for (SseConnectionManager.SubscriberKey key : connections.subscriberKeys()) {
            push(key);
        }
    }

    private void push(SseConnectionManager.SubscriberKey key) {
        try {
            QueueSnapshot snapshot = snapshots.snapshot(key.performanceId(), key.subscriberId());
            connections.emit(key, SseEvent.of("queue", snapshot, snapshot.capturedAt().toEpochMilli()));
        } catch (RuntimeException exception) {
            LOGGER.warn("Queue snapshot refresh failed performanceId={}", key.performanceId());
        }
    }
}
