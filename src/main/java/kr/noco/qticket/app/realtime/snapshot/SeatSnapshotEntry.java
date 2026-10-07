package kr.noco.qticket.app.realtime.snapshot;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;

/** 좌석 1개의 스냅샷 항목. holderId 등 actor 식별자는 담지 않는다(비노출 계약). */
public record SeatSnapshotEntry(Long seatId, String saleStatus, boolean held, long holdTtlMillis) {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String SOLD = "SOLD";

    public SeatSnapshotEntry {
        if (seatId == null || seatId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (!AVAILABLE.equals(saleStatus) && !SOLD.equals(saleStatus)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (holdTtlMillis < 0) {
            holdTtlMillis = 0L;
        }
        held = held && holdTtlMillis > 0;
    }
}
