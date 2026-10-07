package kr.noco.qticket.app.hold;

import kr.noco.qticket.app.realtime.SeatRealtimeEvent;
import kr.noco.qticket.app.realtime.SeatRealtimeEvents;
import kr.noco.qticket.app.realtime.SeatRealtimePublisherPort;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.queue.QueueAdmissionPort;
import kr.noco.qticket.app.validation.ExternalIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SeatHoldService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeatHoldService.class);
    private final SeatHoldPort holds;
    private final QueueAdmissionPort admissions;
    private final SeatRealtimePublisherPort events;

    public SeatHoldService(SeatHoldPort holds, QueueAdmissionPort admissions,
                           SeatRealtimePublisherPort events) {
        this.holds = holds;
        this.admissions = admissions;
        this.events = events;
    }

    public boolean hold(Long performanceId, Long seatId, String holderId) {
        validate(performanceId, seatId, holderId);
        requireActive(performanceId, holderId);
        boolean acquired = holds.hold(performanceId, seatId, holderId);
        if (acquired) {
            publish(performanceId, seatId, SeatRealtimeEvent.Type.HOLD);
        }
        return acquired;
    }

    public boolean release(Long performanceId, Long seatId, String holderId) {
        validate(performanceId, seatId, holderId);
        boolean released = holds.release(performanceId, seatId, holderId);
        if (released) {
            publish(performanceId, seatId, SeatRealtimeEvent.Type.RELEASE);
        }
        return released;
    }

    private void requireActive(Long performanceId, String holderId) {
        if (!admissions.isActive(performanceId, holderId)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    private void publish(Long performanceId, Long seatId, SeatRealtimeEvent.Type type) {
        try {
            events.publish(SeatRealtimeEvents.of(performanceId, seatId, type));
        } catch (RuntimeException exception) {
            LOGGER.warn("Seat realtime publication failed performanceId={} seatId={}",
                    performanceId, seatId, exception);
        }
    }

    private void validate(Long performanceId, Long seatId, String holderId) {
        if (performanceId == null || performanceId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (seatId == null || seatId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        ExternalIdentifier.requireValid(holderId);
    }
}
