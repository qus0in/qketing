package kr.noco.qticket.domain.booking;

import java.util.Objects;

/** acquired is true only for the transaction that atomically inserted the idempotency key. */
public record BookingClaim(Booking booking, boolean acquired) {

    public BookingClaim {
        Objects.requireNonNull(booking);
        if (booking.id() == null) {
            throw new IllegalArgumentException("Claim must contain a persisted booking");
        }
    }
}
