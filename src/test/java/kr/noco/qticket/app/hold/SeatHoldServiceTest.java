package kr.noco.qticket.app.hold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import kr.noco.qticket.app.queue.QueueAdmissionPort;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.SeatRealtimePublisherPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SeatHoldServiceTest {

    private final SeatHoldPort holds = mock(SeatHoldPort.class);
    private final QueueAdmissionPort admissions = mock(QueueAdmissionPort.class);
    private final SeatRealtimePublisherPort realtime = mock(SeatRealtimePublisherPort.class);
    private final SeatHoldService service = new SeatHoldService(holds, admissions, realtime);

    @Test
    void givenHoldAcquired_whenHeld_thenPublishesHoldEvent() {
        given(admissions.isActive(7L, "owner-1")).willReturn(true);
        given(holds.hold(7L, 42L, "owner-1")).willReturn(true);

        assertThat(service.hold(7L, 42L, "owner-1")).isTrue();

        assertPublished(SeatRealtimeEvent.Type.HOLD);
    }

    @Test
    void givenHoldNotAcquired_whenHeld_thenDoesNotPublish() {
        given(admissions.isActive(7L, "owner-1")).willReturn(true);
        given(holds.hold(7L, 42L, "owner-1")).willReturn(false);

        assertThat(service.hold(7L, 42L, "owner-1")).isFalse();

        verifyNoInteractions(realtime);
    }

    @Test
    void givenHoldReleased_whenReleased_thenPublishesReleaseEvent() {
        given(holds.release(7L, 42L, "owner-1")).willReturn(true);

        assertThat(service.release(7L, 42L, "owner-1")).isTrue();

        assertPublished(SeatRealtimeEvent.Type.RELEASE);
    }

    private void assertPublished(SeatRealtimeEvent.Type type) {
        ArgumentCaptor<SeatRealtimeEvent> event = ArgumentCaptor.forClass(SeatRealtimeEvent.class);
        verify(realtime).publish(event.capture());
        assertThat(event.getValue().type()).isEqualTo(type);
        assertThat(event.getValue().performanceId()).isEqualTo(7L);
        assertThat(event.getValue().seatId()).isEqualTo(42L);
    }
}
