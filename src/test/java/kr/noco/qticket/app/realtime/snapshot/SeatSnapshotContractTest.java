package kr.noco.qticket.app.realtime.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class SeatSnapshotContractTest {

    private final SeatSaleSnapshotReadPort sales = mock(SeatSaleSnapshotReadPort.class);
    private final HoldTtlSnapshotReadPort holds = mock(HoldTtlSnapshotReadPort.class);
    private final SeatSnapshotService service = new SeatSnapshotService(sales, holds);

    @Test
    void givenSnapshot_whenMutatingSeats_thenUnsupported() {
        SeatSnapshot snapshot = new SeatSnapshot(1L,
                List.of(new SeatSnapshotEntry(1L, "AVAILABLE", false, 0L)), Instant.now());

        assertThat(snapshot.seats()).hasSize(1);
        assertThatThrownBy(() -> snapshot.seats().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(snapshot.seats())
                .isEqualTo(List.of(new SeatSnapshotEntry(1L, "AVAILABLE", false, 0L)));
    }

    @Test
    void givenNonPositivePerformanceId_whenSnapshot_thenInvalidInput() {
        assertInvalidInput(() -> service.snapshot(0L));
        assertInvalidInput(() -> service.snapshot(null));
        verifyNoInteractions(sales, holds);
    }

    @Test
    void givenInvalidEntryValues_whenCreated_thenInvalidInput() {
        assertInvalidInput(() -> new SeatSnapshotEntry(0L, "AVAILABLE", false, 0L));
        assertInvalidInput(() -> new SeatSnapshotEntry(1L, "RESERVED", false, 0L));
        assertInvalidInput(() -> new SeatSaleState(null, "AVAILABLE"));
        assertInvalidInput(() -> new SeatSnapshot(0L, List.of(), Instant.now()));
    }

    @Test
    void givenNegativeTtlOrUnheldFlag_whenCreated_thenNormalized() {
        SeatSnapshotEntry negativeTtl = new SeatSnapshotEntry(1L, "AVAILABLE", true, -5L);
        assertThat(negativeTtl.holdTtlMillis()).isZero();
        assertThat(negativeTtl.held()).isFalse();

        SeatSnapshotEntry zeroTtlHeld = new SeatSnapshotEntry(2L, "AVAILABLE", true, 0L);
        assertThat(zeroTtlHeld.held()).isFalse();
    }

    @Test
    void givenEmptySeats_whenSnapshot_thenEmptyImmutableList() {
        given(sales.findByPerformanceId(3L)).willReturn(List.of());
        given(holds.holdTtlMillis(3L, List.of())).willReturn(Map.of());

        List<SeatSnapshotEntry> seats = service.snapshot(3L).seats();

        assertThat(seats).isEmpty();
        assertThatThrownBy(() -> seats.add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    private static void assertInvalidInput(ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(
                        error.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
