package kr.noco.qticket.domain.booking;

public record Booking(Long id, String idempotencyKey, Long performanceId, Long seatId) {

    public Booking {
        if (id != null && id < 1) {
            throw new IllegalArgumentException("Booking id must be positive");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency key is invalid");
        }
        if (performanceId == null || performanceId < 1 || seatId == null || seatId < 1) {
            throw new IllegalArgumentException("Booking identifiers must be positive");
        }
    }

    public static Booking request(String key, Long performanceId, Long seatId) {
        return new Booking(null, key, performanceId, seatId);
    }

    public boolean matches(Long requestedPerformanceId, Long requestedSeatId) {
        return performanceId.equals(requestedPerformanceId) && seatId.equals(requestedSeatId);
    }
}
