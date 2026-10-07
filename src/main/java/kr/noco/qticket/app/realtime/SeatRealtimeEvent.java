package kr.noco.qticket.app.realtime;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SeatRealtimeEvent(UUID eventId, Long performanceId, Long seatId, Type type,
                                Instant occurredAt) {

    public SeatRealtimeEvent {
        Objects.requireNonNull(eventId, "eventId");
        if (performanceId == null || performanceId < 1 || seatId == null || seatId < 1) {
            throw new IllegalArgumentException("Seat event identifiers must be positive");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public enum Type {
        HOLD,
        RELEASE,
        SOLD
    }
}
