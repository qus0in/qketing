package kr.noco.qticket.app.queue.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

/** QueueSnapshotService 검증과 위임. */
class QueueSnapshotServiceTest {

    private final QueueSnapshotReadPort reads = mock(QueueSnapshotReadPort.class);
    private final QueueSnapshotService service = new QueueSnapshotService(reads);

    @Test
    void givenValidRequest_whenSnapshot_thenDelegatesToPort() {
        QueueSnapshot expected = snapshot();
        given(reads.read(7L, "member-1")).willReturn(expected);

        assertThat(service.snapshot(7L, "member-1")).isEqualTo(expected);
        verify(reads).read(7L, "member-1");
    }

    @Test
    void givenInvalidInput_whenSnapshot_thenInvalidInputWithoutReading() {
        assertInvalid(() -> service.snapshot(null, "member-1"));
        assertInvalid(() -> service.snapshot(0L, "member-1"));
        assertInvalid(() -> service.snapshot(7L, "bad id!"));
        assertInvalid(() -> service.snapshot(7L, "a/b"));

        verifyNoInteractions(reads);
    }

    private void assertInvalid(ThrowingCallable callable) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(callable)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private QueueSnapshot snapshot() {
        return new QueueSnapshot(7L, QueueSnapshot.Status.WAITING, 3, 0L,
                Instant.parse("2026-10-06T00:00:00Z"));
    }
}
