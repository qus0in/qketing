package kr.noco.qticket.app.booking;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.hold.SeatHoldPort;
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
    private final SeatHoldPort holds;
    private final BookingCommitEffects commitEffects;

    public ConfirmBookingService(BookingPersistencePort bookings, SeatPersistencePort seats,
            SeatHoldPort holds, BookingCommitEffects commitEffects) {
        this.bookings = bookings;
        this.seats = seats;
        this.holds = holds;
        this.commitEffects = commitEffects;
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
        return claim.acquired() ? sell(booking, command.holderId()) : replay(booking);
    }

    private BookingResult sell(Booking booking, String holderId) {
        requireHoldOwner(booking, holderId);
        if (!seats.claimForSale(booking.performanceId(), booking.seatId())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        Ticket ticket = bookings.save(Ticket.issue(booking.id(), booking.performanceId(),
                booking.seatId()));
        commitEffects.releaseHoldAndPublishSold(booking.performanceId(), booking.seatId(), holderId);
        return BookingResult.from(booking, ticket);
    }

    private BookingResult replay(Booking booking) {
        Ticket ticket = bookings.findTicketByBookingId(booking.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL));
        return BookingResult.from(booking, ticket);
    }

    private void requireHoldOwner(Booking booking, String holderId) {
        if (!holds.isHeldBy(booking.performanceId(), booking.seatId(), holderId)) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    private void validateSameRequest(Booking booking, ConfirmBookingCommand command) {
        if (!booking.matches(command.performanceId(), command.seatId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
