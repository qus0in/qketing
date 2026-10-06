package kr.noco.qticket.domain.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PerformanceTest {

    private static final Instant STARTS_AT = Instant.parse("2026-11-01T10:00:00Z");

    @Test
    void givenValidValues_whenCreated_thenFieldsStored() {
        Performance performance = new Performance(1L, "오페라의 유령", STARTS_AT);

        assertThat(performance.id()).isEqualTo(1L);
        assertThat(performance.title()).isEqualTo("오페라의 유령");
        assertThat(performance.startsAt()).isEqualTo(STARTS_AT);
    }

    @Test
    void givenNullOrNonPositiveId_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Performance(null, "공연", STARTS_AT));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Performance(0L, "공연", STARTS_AT));
    }

    @Test
    void givenNullBlankOrTooLongTitle_whenCreated_thenIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Performance(1L, null, STARTS_AT));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Performance(1L, "   ", STARTS_AT));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new Performance(1L, "a".repeat(201), STARTS_AT));
    }

    @Test
    void givenTitleAtMaxLength_whenCreated_thenAccepted() {
        Performance performance = new Performance(1L, "a".repeat(200), STARTS_AT);

        assertThat(performance.title()).hasSize(200);
    }

    @Test
    void givenNullStartsAt_whenCreated_thenNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Performance(1L, "공연", null));
    }
}
