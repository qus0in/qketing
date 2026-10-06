package kr.noco.qticket.app.seat;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.domain.seat.SeatSort;
import kr.noco.qticket.domain.seat.SortDirection;

public record ListSeatsQuery(Long performanceId, SeatSort sort, SortDirection direction) {

    public ListSeatsQuery {
        if (performanceId == null || performanceId <= 0 || sort == null || direction == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
