package kr.noco.qticket.ui.realtime.api;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.Instant;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshot;
import kr.noco.qticket.app.queue.snapshot.QueueSnapshotService;
import kr.noco.qticket.ui.realtime.sse.SseConnectionManager;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

/** QueueStreamController 입력 검증과 snapshot supplier 연결. */
class QueueStreamControllerTest {

    private static final QueueStreamProperties PROPERTIES = new QueueStreamProperties(
            Duration.ofSeconds(15), Duration.ofSeconds(3), Duration.ofMinutes(30));

    @Test
    void givenInvalidInput_whenStream_thenInvalidInputWithoutSnapshot() {
        QueueSnapshotService snapshots = mock(QueueSnapshotService.class);
        QueueStreamController controller = controller(snapshots);

        assertInvalid(() -> controller.stream(null, "member-1"));
        assertInvalid(() -> controller.stream(0L, "member-1"));
        assertInvalid(() -> controller.stream(7L, "bad id!"));

        verifyNoInteractions(snapshots);
    }

    @Test
    void givenValidRequest_whenStream_thenSnapshotSupplierInvoked() {
        QueueSnapshotService snapshots = mock(QueueSnapshotService.class);
        given(snapshots.snapshot(7L, "member-1")).willReturn(snapshot());
        QueueStreamController controller = controller(snapshots);

        controller.stream(7L, "member-1");

        verify(snapshots).snapshot(7L, "member-1");
    }

    private QueueStreamController controller(QueueSnapshotService snapshots) {
        return new QueueStreamController(snapshots, new SseConnectionManager(), PROPERTIES);
    }

    private void assertInvalid(ThrowingCallable callable) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(callable)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private QueueSnapshot snapshot() {
        return new QueueSnapshot(7L, QueueSnapshot.Status.WAITING, 2, 0L,
                Instant.parse("2026-10-06T00:00:00Z"));
    }
}
