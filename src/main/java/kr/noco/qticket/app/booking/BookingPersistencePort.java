package kr.noco.qticket.app.booking;

import java.util.Optional;
import kr.noco.qticket.domain.booking.Booking;
import kr.noco.qticket.domain.booking.BookingClaim;
import kr.noco.qticket.domain.booking.Ticket;

public interface BookingPersistencePort {

    /**
     * Claims the global key in the caller transaction. A new insert returns acquired=true;
     * a conflict waits for commit and returns the committed booking with acquired=false.
     * The caller then reads its ticket in the next READ COMMITTED statement.
     */
    BookingClaim claimIdempotencyKey(Booking request);

    Ticket save(Ticket ticket);

    Optional<Ticket> findTicketByBookingId(Long bookingId);
}
