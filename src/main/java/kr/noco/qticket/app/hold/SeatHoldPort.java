package kr.noco.qticket.app.hold;

public interface SeatHoldPort {

    boolean hold(Long performanceId, Long seatId, String holderId);

    boolean isHeldBy(Long performanceId, Long seatId, String holderId);

    boolean release(Long performanceId, Long seatId, String holderId);
}
