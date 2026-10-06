package kr.noco.qticket.app.performance;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;

public record FindPerformanceQuery(Long performanceId) {

    public FindPerformanceQuery {
        if (performanceId == null || performanceId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
