package kr.noco.qticket.domain.seat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SeatTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-06T00:00:00Z");

    @Test
    void givenValidValues_whenCreated_thenFieldsStored() {
        Seat seat = new Seat(1L, 10L, "A", "1", 5, CREATED_AT);

        assertThat(seat.section()).isEqualTo("A");
        assertThat(seat.rowLabel()).isEqualTo("1");
        assertThat(seat.seatNumber()).isEqualTo(5);
        assertThat(seat.performanceId()).isEqualTo(10L);
    }

    @Test
    void givenNullOrNonPositiveIdentifiers_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(null, 10L, "A", "1", 1, CREATED_AT));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(1L, 0L, "A", "1", 1, CREATED_AT));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(1L, null, "A", "1", 1, CREATED_AT));
    }

    @Test
    void givenInvalidSection_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(1L, 10L, null, "1", 1, CREATED_AT));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(1L, 10L, " ", "1", 1, CREATED_AT));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(1L, 10L, "a".repeat(17), "1", 1, CREATED_AT));
    }

    @Test
    void givenInvalidRowLabel_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(1L, 10L, "A", null, 1, CREATED_AT));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(1L, 10L, "A", "", 1, CREATED_AT));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(1L, 10L, "A", "a".repeat(9), 1, CREATED_AT));
    }

    @Test
    void givenNullOrNonPositiveSeatNumber_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(1L, 10L, "A", "1", null, CREATED_AT));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Seat(1L, 10L, "A", "1", 0, CREATED_AT));
    }

    @Test
    void givenNullCreatedAt_whenCreated_thenNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Seat(1L, 10L, "A", "1", 1, null));
    }
}
