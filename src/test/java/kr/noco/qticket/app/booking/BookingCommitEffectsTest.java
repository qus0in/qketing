package kr.noco.qticket.app.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import kr.noco.qticket.app.hold.SeatHoldPort;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.SeatRealtimePublisherPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class BookingCommitEffectsTest {

    private final SeatHoldPort holds = mock(SeatHoldPort.class);
    private final SeatRealtimePublisherPort events = mock(SeatRealtimePublisherPort.class);
    private final BookingCommitEffects effects = new BookingCommitEffects(holds, events);

    @Test
    void givenRegisteredEffect_whenAfterCommit_thenReleasesAndPublishesSold() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            effects.releaseHoldAndPublishSold(7L, 42L, "owner-1");
            verifyNoInteractions(holds, events);
            TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();
            verifyReleaseAndSoldEvent();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void givenPublisherFailure_whenAfterCommit_thenDatabaseResultIsUnaffected() {
        doThrow(new IllegalStateException("pubsub unavailable"))
                .when(events).publish(any(SeatRealtimeEvent.class));
        TransactionSynchronizationManager.initSynchronization();
        try {
            effects.releaseHoldAndPublishSold(7L, 42L, "owner-1");
            TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();
            verify(holds).release(7L, 42L, "owner-1");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void verifyReleaseAndSoldEvent() {
        ArgumentCaptor<SeatRealtimeEvent> event = ArgumentCaptor.forClass(SeatRealtimeEvent.class);
        InOrder order = inOrder(holds, events);
        order.verify(holds).release(7L, 42L, "owner-1");
        order.verify(events).publish(event.capture());
        assertThat(event.getValue().type()).isEqualTo(SeatRealtimeEvent.Type.SOLD);
    }
}
