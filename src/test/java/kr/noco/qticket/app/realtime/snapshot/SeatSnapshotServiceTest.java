package kr.noco.qticket.app.realtime.snapshot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SeatSnapshotServiceTest {

    private static final Long PID = 7L;

    private final SeatSaleSnapshotReadPort sales = mock(SeatSaleSnapshotReadPort.class);
    private final HoldTtlSnapshotReadPort holds = mock(HoldTtlSnapshotReadPort.class);
    private final SeatSnapshotService service = new SeatSnapshotService(sales, holds);

    @Test
    void givenAvailableWithoutHold_whenSnapshot_thenHeldFalse() {
        givenSales(new SeatSaleState(2L, "AVAILABLE"));
        given(holds.holdTtlMillis(PID, List.of(2L))).willReturn(Map.of());

        SeatSnapshot snapshot = service.snapshot(PID);

        assertThat(snapshot.seats())
                .containsExactly(new SeatSnapshotEntry(2L, "AVAILABLE", false, 0L));
        assertThat(snapshot.performanceId()).isEqualTo(PID);
        assertThat(snapshot.capturedAt()).isNotNull();
    }

    @Test
    void givenAvailableWithHoldTtl_whenSnapshot_thenHeldWithTtl() {
        givenSales(new SeatSaleState(3L, "AVAILABLE"));
        given(holds.holdTtlMillis(PID, List.of(3L))).willReturn(Map.of(3L, 4500L));

        SeatSnapshot snapshot = service.snapshot(PID);

        assertThat(snapshot.seats())
                .containsExactly(new SeatSnapshotEntry(3L, "AVAILABLE", true, 4500L));
    }

    @Test
    void givenExpiredHoldTtl_whenSnapshot_thenHeldFalse() {
        givenSales(new SeatSaleState(4L, "AVAILABLE"));
        given(holds.holdTtlMillis(PID, List.of(4L))).willReturn(Map.of(4L, 0L));

        assertThat(service.snapshot(PID).seats())
                .containsExactly(new SeatSnapshotEntry(4L, "AVAILABLE", false, 0L));
    }

    @Test
    void givenSoldSeatWithTtl_whenSnapshot_thenSoldWinsWithZeroTtl() {
        givenSales(new SeatSaleState(5L, "SOLD"), new SeatSaleState(6L, "AVAILABLE"));
        given(holds.holdTtlMillis(PID, List.of(5L, 6L))).willReturn(Map.of(5L, 3000L, 6L, 2000L));

        List<SeatSnapshotEntry> seats = service.snapshot(PID).seats();

        assertThat(seats).containsExactly(
                new SeatSnapshotEntry(5L, "SOLD", false, 0L),
                new SeatSnapshotEntry(6L, "AVAILABLE", true, 2000L));
    }

    @Test
    void givenUnorderedSales_whenSnapshot_thenSortedBySeatId() {
        givenSales(new SeatSaleState(9L, "AVAILABLE"), new SeatSaleState(1L, "SOLD"));
        given(holds.holdTtlMillis(PID, List.of(1L, 9L))).willReturn(Map.of(9L, 1000L));

        assertThat(service.snapshot(PID).seats()).extracting(SeatSnapshotEntry::seatId)
                .containsExactly(1L, 9L);
    }

    private void givenSales(SeatSaleState... states) {
        given(sales.findByPerformanceId(PID)).willReturn(List.of(states));
    }
}
