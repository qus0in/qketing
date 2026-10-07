package kr.noco.qticket.ui.realtime.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshot;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshotService;
import kr.noco.qticket.app.realtime.QueueRealtimeEvent;
import kr.noco.qticket.domain.queue.AdmissionStatus;
import kr.noco.qticket.ui.realtime.sse.SseConnectionManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

/** bridge가 원본 재조회 후 해당 subscriber 키로만, memberId 없이 전달한다. */
class QueueRealtimeEventBridgeTest {

    private static final Long PERFORMANCE_ID = 7L;

    @Test
    void givenEvent_whenBridged_thenOnlyTargetSubscriberGetsSourceSnapshot() throws IOException {
        QueueSnapshotService snapshots = mock(QueueSnapshotService.class);
        QueueSnapshot source = new QueueSnapshot(PERFORMANCE_ID, QueueSnapshot.Status.WAITING, 1, 0L,
                Instant.parse("2026-10-07T00:00:00Z"));
        given(snapshots.snapshot(PERFORMANCE_ID, "member-a")).willReturn(source);
        SseConnectionManager connections = new SseConnectionManager();
        SseEmitter target = mock(SseEmitter.class);
        SseEmitter other = mock(SseEmitter.class);
        List<Object> payloads = new ArrayList<>();
        recordData(target, payloads);
        connections.register(subscriber("member-a"), target);
        connections.register(subscriber("member-b"), other);

        new QueueRealtimeEventBridge(snapshots, connections).onQueueRealtimeEvent(
                new QueueRealtimeEvent(PERFORMANCE_ID, "member-a", AdmissionStatus.ACTIVE, 123L));

        verify(snapshots).snapshot(PERFORMANCE_ID, "member-a");
        verify(target).send(any(SseEventBuilder.class));
        verify(other, never()).send(any(SseEventBuilder.class));
        assertThat(payloads).containsExactly(source);
    }

    private static SseConnectionManager.SubscriberKey subscriber(String memberId) {
        return new SseConnectionManager.SubscriberKey(PERFORMANCE_ID, memberId);
    }

    private static void recordData(SseEmitter emitter, List<Object> data) {
        try {
            doAnswer(invocation -> {
                SseEventBuilder builder = invocation.getArgument(0);
                builder.build().stream().map(DataWithMediaType::getData)
                        .filter(value -> !(value instanceof String)).forEach(data::add);
                return null;
            }).when(emitter).send(any(SseEventBuilder.class));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
