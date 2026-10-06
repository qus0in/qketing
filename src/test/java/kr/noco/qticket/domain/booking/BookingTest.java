package kr.noco.qticket.domain.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

class BookingTest {

    @Test
    void givenValidValues_whenCreated_thenFieldsStored() {
        Booking booking = new Booking(1L, "key-1", 10L, 20L);

        assertThat(booking.id()).isEqualTo(1L);
        assertThat(booking.idempotencyKey()).isEqualTo("key-1");
        assertThat(booking.performanceId()).isEqualTo(10L);
        assertThat(booking.seatId()).isEqualTo(20L);
    }

    @Test
    void givenNonPositiveId_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Booking(0L, "key-1", 10L, 20L));
    }

    @Test
    void givenNullOrBlankOrTooLongKey_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Booking(1L, null, 10L, 20L));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Booking(1L, " ", 10L, 20L));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Booking(1L, "k".repeat(129), 10L, 20L));
    }

    @Test
    void givenNullOrNonPositiveIdentifiers_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Booking(1L, "key-1", null, 20L));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Booking(1L, "key-1", 0L, 20L));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Booking(1L, "key-1", 10L, -1L));
    }

    @Test
    void givenRequest_whenCalled_thenIdIsNullAndIdentifiersStored() {
        Booking booking = Booking.request("key-1", 10L, 20L);

        assertThat(booking.id()).isNull();
        assertThat(booking.performanceId()).isEqualTo(10L);
        assertThat(booking.seatId()).isEqualTo(20L);
    }

    @Test
    void givenRequestedIdentifiers_whenMatches_thenTrueOnlyForSamePerformanceAndSeat() {
        Booking booking = Booking.request("key-1", 10L, 20L);

        assertThat(booking.matches(10L, 20L)).isTrue();
        assertThat(booking.matches(10L, 21L)).isFalse();
        assertThat(booking.matches(11L, 20L)).isFalse();
    }
}
