package kr.noco.qticket.app.booking;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;

public record ConfirmBookingCommand(Long performanceId, Long seatId, String idempotencyKey) {

    public ConfirmBookingCommand {
        if (performanceId == null || performanceId <= 0 || seatId == null || seatId <= 0
                || idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 128) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
