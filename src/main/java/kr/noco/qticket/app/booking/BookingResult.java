package kr.noco.qticket.app.booking;

import kr.noco.qticket.domain.booking.Booking;
import kr.noco.qticket.domain.booking.Ticket;

/** Stable booking and ticket identifiers; an identical idempotent replay returns the same values. */
public record BookingResult(Long bookingId, Long ticketId, Long performanceId, Long seatId) {

    public static BookingResult from(Booking booking, Ticket ticket) {
        return new BookingResult(booking.id(), ticket.id(), booking.performanceId(), booking.seatId());
    }
}
