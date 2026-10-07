package kr.noco.qticket.app.booking;

import kr.noco.qticket.app.hold.SeatHoldPort;
import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.SeatRealtimeEvents;
import kr.noco.qticket.app.realtime.SeatRealtimePublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class BookingCommitEffects {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingCommitEffects.class);
    private final SeatHoldPort holds;
    private final SeatRealtimePublisherPort events;

    public BookingCommitEffects(SeatHoldPort holds, SeatRealtimePublisherPort events) {
        this.holds = holds;
        this.events = events;
    }

    public void releaseHoldAndPublishSold(Long performanceId, Long seatId, String holderId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                releaseHold(performanceId, seatId, holderId);
                publishSold(performanceId, seatId);
            }
        });
    }

    private void releaseHold(Long performanceId, Long seatId, String holderId) {
        try {
            holds.release(performanceId, seatId, holderId);
        } catch (RuntimeException exception) {
            LOGGER.warn("Seat hold release failed performanceId={} seatId={}",
                    performanceId, seatId, exception);
        }
    }

    private void publishSold(Long performanceId, Long seatId) {
        try {
            events.publish(SeatRealtimeEvents.of(performanceId, seatId, SeatRealtimeEvent.Type.SOLD));
        } catch (RuntimeException exception) {
            LOGGER.warn("Seat sold event publication failed performanceId={} seatId={}",
                    performanceId, seatId, exception);
        }
    }
}
