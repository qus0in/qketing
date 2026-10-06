package kr.noco.qticket.infra.persistence.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.noco.qticket.TestcontainersConfiguration;
import kr.noco.qticket.infra.persistence.performance.PerformanceEntity;
import kr.noco.qticket.infra.persistence.seat.SeatEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/** V3 booking/ticket schema 영속 제약 검증 (#33 조각 12). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BookingPersistenceSmokeTest extends BookingPersistenceSupport {

    @Test
    void givenBookingAndTicket_whenSaved_thenFindable() {
        PerformanceEntity performance = createPerformance();
        SeatEntity seat = createSeat(performance.getId(), 1);
        BookingEntity booking = bookingRepository.saveAndFlush(
                BookingEntity.of("key-1", performance.getId(), seat.getId()));
        ticketRepository.saveAndFlush(
                TicketEntity.of(booking.getId(), performance.getId(), seat.getId()));

        assertThat(bookingRepository.findByIdempotencyKey("key-1")).contains(booking);
        assertThat(ticketRepository.findByBookingId(booking.getId())).isPresent();
    }

    @Test
    void givenSameIdempotencyKey_whenSavedTwice_thenDataIntegrityViolation() {
        PerformanceEntity performance = createPerformance();
        SeatEntity seat = createSeat(performance.getId(), 1);
        bookingRepository.saveAndFlush(
                BookingEntity.of("dup-key", performance.getId(), seat.getId()));

        assertThatThrownBy(() -> bookingRepository.saveAndFlush(
                BookingEntity.of("dup-key", performance.getId(), seat.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenSameSeat_whenTwoTicketsSaved_thenDataIntegrityViolation() {
        PerformanceEntity performance = createPerformance();
        SeatEntity seat = createSeat(performance.getId(), 1);
        BookingEntity first = bookingRepository.saveAndFlush(
                BookingEntity.of("key-a", performance.getId(), seat.getId()));
        BookingEntity second = bookingRepository.saveAndFlush(
                BookingEntity.of("key-b", performance.getId(), seat.getId()));
        ticketRepository.saveAndFlush(
                TicketEntity.of(first.getId(), performance.getId(), seat.getId()));

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(
                TicketEntity.of(second.getId(), performance.getId(), seat.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenSeatInsertedWithoutSaleStatus_whenQueried_thenDefaultsToAvailable() {
        PerformanceEntity performance = createPerformance();
        jdbcTemplate.update("insert into seat (performance_id, section, row_label, seat_number)"
                + " values (?, ?, ?, ?)", performance.getId(), "A", "A", 1);

        String saleStatus = jdbcTemplate.queryForObject(
                "select sale_status from seat where performance_id = ? and seat_number = 1",
                String.class, performance.getId());

        assertThat(saleStatus).isEqualTo("AVAILABLE");
    }
}
