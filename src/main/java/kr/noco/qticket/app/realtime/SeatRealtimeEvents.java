package kr.noco.qticket.app.realtime;

import java.time.Instant;
import java.util.UUID;

public final class SeatRealtimeEvents {

    private SeatRealtimeEvents() {
    }

    public static SeatRealtimeEvent of(Long performanceId, Long seatId,
                                       SeatRealtimeEvent.Type type) {
        return new SeatRealtimeEvent(UUID.randomUUID(), performanceId, seatId, type, Instant.now());
    }
}
