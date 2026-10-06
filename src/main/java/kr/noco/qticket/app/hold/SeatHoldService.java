package kr.noco.qticket.app.hold;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.queue.QueueAdmissionPort;
import kr.noco.qticket.app.validation.ExternalIdentifier;
import org.springframework.stereotype.Service;

@Service
public class SeatHoldService {

    private final SeatHoldPort holds;
    private final QueueAdmissionPort admissions;

    public SeatHoldService(SeatHoldPort holds, QueueAdmissionPort admissions) {
        this.holds = holds;
        this.admissions = admissions;
    }

    public boolean hold(Long performanceId, Long seatId, String holderId) {
        validate(performanceId, seatId, holderId);
        requireActive(performanceId, holderId);
        return holds.hold(performanceId, seatId, holderId);
    }

    public boolean release(Long performanceId, Long seatId, String holderId) {
        validate(performanceId, seatId, holderId);
        return holds.release(performanceId, seatId, holderId);
    }

    private void requireActive(Long performanceId, String holderId) {
        if (!admissions.isActive(performanceId, holderId)) {
            throw new BusinessException(ErrorCode.CONFLICT);
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
