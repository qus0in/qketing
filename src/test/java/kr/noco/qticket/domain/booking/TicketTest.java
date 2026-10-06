package kr.noco.qticket.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

class TicketTest {

    @Test
    void givenValidValues_whenCreated_thenFieldsStored() {
        Ticket ticket = new Ticket(1L, 100L, 10L, 20L);

        assertThat(ticket.id()).isEqualTo(1L);
        assertThat(ticket.bookingId()).isEqualTo(100L);
        assertThat(ticket.performanceId()).isEqualTo(10L);
        assertThat(ticket.seatId()).isEqualTo(20L);
    }

    @Test
    void givenNonPositiveId_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Ticket(0L, 100L, 10L, 20L));
    }

    @Test
    void givenNullOrNonPositiveBookingId_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Ticket(1L, null, 10L, 20L));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Ticket(1L, 0L, 10L, 20L));
    }

    @Test
    void givenNullOrNonPositivePerformanceId_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Ticket(1L, 100L, null, 20L));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Ticket(1L, 100L, -1L, 20L));
    }

    @Test
    void givenNullOrNonPositiveSeatId_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Ticket(1L, 100L, 10L, null));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Ticket(1L, 100L, 10L, 0L));
    }

    @Test
    void givenIssue_whenCalled_thenIdIsNullAndIdentifiersStored() {
        Ticket ticket = Ticket.issue(100L, 10L, 20L);

        assertThat(ticket.id()).isNull();
        assertThat(ticket.bookingId()).isEqualTo(100L);
        assertThat(ticket.performanceId()).isEqualTo(10L);
        assertThat(ticket.seatId()).isEqualTo(20L);
    }
}
