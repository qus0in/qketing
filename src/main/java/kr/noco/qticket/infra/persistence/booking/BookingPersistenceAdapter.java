package kr.noco.qticket.infra.persistence.booking;

import java.util.List;
import java.util.Optional;
import kr.noco.qticket.app.booking.BookingPersistencePort;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.domain.booking.Booking;
import kr.noco.qticket.domain.booking.BookingClaim;
import kr.noco.qticket.domain.booking.Ticket;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingPersistenceAdapter implements BookingPersistencePort {

    private static final String CLAIM_SQL = "INSERT INTO booking "
            + "(idempotency_key, performance_id, seat_id) VALUES (?, ?, ?) "
            + "ON CONFLICT (idempotency_key) DO NOTHING RETURNING id";

    private final JdbcTemplate jdbcTemplate;
    private final BookingJpaRepository bookings;
    private final TicketJpaRepository tickets;

    public BookingPersistenceAdapter(JdbcTemplate jdbcTemplate, BookingJpaRepository bookings,
                                     TicketJpaRepository tickets) {
        this.jdbcTemplate = jdbcTemplate;
        this.bookings = bookings;
        this.tickets = tickets;
    }

    @Override
    public BookingClaim claimIdempotencyKey(Booking request) {
        try {
            return claim(request);
        } catch (DataIntegrityViolationException exception) {
            throw translateBookingSeatViolation(exception);
        }
    }

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity saved = tickets.save(TicketEntity.of(ticket.bookingId(),
                ticket.performanceId(), ticket.seatId()));
        return toDomain(saved);
    }

    @Override
    public Optional<Ticket> findTicketByBookingId(Long bookingId) {
        return tickets.findByBookingId(bookingId).map(this::toDomain);
    }

    private BookingClaim claim(Booking request) {
        Long insertedId = insert(request);
        if (insertedId != null) {
            Booking created = new Booking(insertedId, request.idempotencyKey(),
                    request.performanceId(), request.seatId());
            return new BookingClaim(created, true);
        }
        BookingEntity existing = bookings.findByIdempotencyKey(request.idempotencyKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL));
        Booking replay = new Booking(existing.getId(), existing.getIdempotencyKey(),
                existing.getPerformanceId(), existing.getSeatId());
        return new BookingClaim(replay, false);
    }

    private Long insert(Booking request) {
        List<Long> ids = jdbcTemplate.query(CLAIM_SQL,
                (result, row) -> result.getLong("id"), request.idempotencyKey(),
                request.performanceId(), request.seatId());
        return ids.isEmpty() ? null : ids.get(0);
    }

    private RuntimeException translateBookingSeatViolation(
            DataIntegrityViolationException exception) {
        if (isBookingSeatViolation(exception)) {
            return new BusinessException(ErrorCode.NOT_FOUND);
        }
        return exception;
    }

    private boolean isBookingSeatViolation(Throwable cause) {
        while (cause != null && !(cause instanceof PSQLException)) {
            cause = cause.getCause();
        }
        if (!(cause instanceof PSQLException psql)) {
            return false;
        }
        ServerErrorMessage error = psql.getServerErrorMessage();
        return error != null && "23503".equals(psql.getSQLState()) && "fk_booking_seat".equals(error.getConstraint());
    }

    private Ticket toDomain(TicketEntity entity) {
        return new Ticket(entity.getId(), entity.getBookingId(), entity.getPerformanceId(),
                entity.getSeatId());
    }
}
