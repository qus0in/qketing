package kr.noco.qticket.app.realtime.snapshot;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;

/** PostgreSQL에서 조회한 좌석 1건의 sale_status 행(원본 조회 결과). */
public record SeatSaleState(Long seatId, String saleStatus) {

    public SeatSaleState {
        if (seatId == null || seatId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (!SeatSnapshotEntry.AVAILABLE.equals(saleStatus)
                && !SeatSnapshotEntry.SOLD.equals(saleStatus)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
