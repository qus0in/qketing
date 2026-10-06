package kr.noco.qticket.app.booking;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.validation.ExternalIdentifier;

public record ConfirmBookingCommand(Long performanceId, Long seatId, String idempotencyKey,
        String holderId) {

    public ConfirmBookingCommand {
        if (performanceId == null || performanceId <= 0 || seatId == null || seatId <= 0
                || idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 128) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        ExternalIdentifier.requireValid(holderId);
    }
}
