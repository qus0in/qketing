package kr.noco.qticket.domain.booking;

public record Ticket(Long id, Long bookingId, Long performanceId, Long seatId) {

    public Ticket {
        if (id != null && id < 1) {
            throw new IllegalArgumentException("Ticket id must be positive");
        }
        if (bookingId == null || bookingId < 1 || performanceId == null || performanceId < 1
                || seatId == null || seatId < 1) {
            throw new IllegalArgumentException("Ticket identifiers must be positive");
        }
    }

    public static Ticket issue(Long bookingId, Long performanceId, Long seatId) {
        return new Ticket(null, bookingId, performanceId, seatId);
    }
}
