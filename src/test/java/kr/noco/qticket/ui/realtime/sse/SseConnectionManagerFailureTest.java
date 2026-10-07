package kr.noco.qticket.ui.realtime.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** snapshot supplier 실패를 숨기지 않고 caller에게 전파하며 registry/emitter를 정리한다. */
class SseConnectionManagerFailureTest {

    private static final Long PERFORMANCE_ID = 7L;
    private final SseConnectionManager manager = new SseConnectionManager();

    @Test
    void givenSupplierFailure_whenRegister_thenRethrowsAndCleansUp() {
        SseEmitter emitter = mock(SseEmitter.class);
        BusinessException failure = new BusinessException(ErrorCode.CONFLICT);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> manager.registerWithSnapshot(key(), emitter, () -> {
                    throw failure;
                }))
                .isSameAs(failure);

        assertThat(manager.subscriberKeys()).isEmpty();
        verify(emitter).complete();
    }

    @Test
    void givenNullSnapshot_whenRegister_thenThrowsAndCleansUp() {
        SseEmitter emitter = mock(SseEmitter.class);

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> manager.registerWithSnapshot(key(), emitter, () -> null));

        assertThat(manager.subscriberKeys()).isEmpty();
        verify(emitter).complete();
    }

    private static SseConnectionManager.SubscriberKey key() {
        return new SseConnectionManager.SubscriberKey(PERFORMANCE_ID, "member-1");
    }
}
