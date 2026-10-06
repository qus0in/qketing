package kr.noco.qticket.app.booking;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.seat.SeatPersistencePort;
import kr.noco.qticket.domain.booking.Booking;
import kr.noco.qticket.domain.booking.BookingClaim;
import kr.noco.qticket.domain.booking.Ticket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmBookingService {

    private final BookingPersistencePort bookings;
    private final SeatPersistencePort seats;

    public ConfirmBookingService(BookingPersistencePort bookings, SeatPersistencePort seats) {
        this.bookings = bookings;
        this.seats = seats;
    }

    @Transactional
    public BookingResult confirm(ConfirmBookingCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Booking request = Booking.request(command.idempotencyKey(), command.performanceId(),
                command.seatId());
        BookingClaim claim = bookings.claimIdempotencyKey(request);
        Booking booking = claim.booking();
        validateSameRequest(booking, command);
        return claim.acquired() ? sell(booking) : replay(booking);
    }

    private BookingResult sell(Booking booking) {
        if (!seats.claimForSale(booking.performanceId(), booking.seatId())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        Ticket ticket = bookings.save(Ticket.issue(booking.id(), booking.performanceId(),
                booking.seatId()));
        return BookingResult.from(booking, ticket);
    }

    private BookingResult replay(Booking booking) {
        Ticket ticket = bookings.findTicketByBookingId(booking.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL));
        return BookingResult.from(booking, ticket);
    }

    private void validateSameRequest(Booking booking, ConfirmBookingCommand command) {
        if (!booking.matches(command.performanceId(), command.seatId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
