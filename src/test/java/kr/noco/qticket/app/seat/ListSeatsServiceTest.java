package kr.noco.qticket.app.seat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.app.performance.PerformancePersistencePort;
import kr.noco.qticket.domain.performance.Performance;
import kr.noco.qticket.domain.seat.Seat;
import kr.noco.qticket.domain.seat.SeatSort;
import kr.noco.qticket.domain.seat.SortDirection;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

class ListSeatsServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-06T00:00:00Z");

    private final SeatPersistencePort seats = mock(SeatPersistencePort.class);
    private final PerformancePersistencePort performances = mock(PerformancePersistencePort.class);
    private final ListSeatsService service = new ListSeatsService(seats, performances);

    @Test
    void givenSeats_whenList_thenResultsMappedInOrder() {
        givenPerformanceExists(7L);
        List<Seat> found = List.of(seat(1L, 1), seat(2L, 2));
        given(seats.findByPerformanceId(7L, SeatSort.POSITION, SortDirection.ASC))
                .willReturn(found);

        List<SeatResult> results = service.list(
                new ListSeatsQuery(7L, SeatSort.POSITION, SortDirection.ASC));

        assertThat(results).extracting(SeatResult::id).containsExactly(1L, 2L);
        assertThat(results).extracting(SeatResult::seatNumber).containsExactly(1, 2);
    }

    @Test
    void givenNoSeats_whenList_thenEmptyResult() {
        givenPerformanceExists(7L);
        given(seats.findByPerformanceId(7L, SeatSort.CREATED_AT, SortDirection.DESC))
                .willReturn(List.of());

        List<SeatResult> results = service.list(
                new ListSeatsQuery(7L, SeatSort.CREATED_AT, SortDirection.DESC));

        assertThat(results).isEmpty();
        verify(seats).findByPerformanceId(7L, SeatSort.CREATED_AT, SortDirection.DESC);
    }

    @Test
    void givenUnknownPerformance_whenList_thenNotFound() {
        given(performances.findById(99L)).willReturn(Optional.empty());

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.list(
                        new ListSeatsQuery(99L, SeatSort.POSITION, SortDirection.ASC)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verifyNoInteractions(seats);
    }

    @Test
    void givenInvalidQuery_whenCreated_thenInvalidInput() {
        assertInvalidInput(() -> new ListSeatsQuery(null, SeatSort.POSITION, SortDirection.ASC));
        assertInvalidInput(() -> new ListSeatsQuery(7L, null, SortDirection.ASC));
        assertInvalidInput(() -> new ListSeatsQuery(7L, SeatSort.POSITION, null));
    }

    private void givenPerformanceExists(Long performanceId) {
        given(performances.findById(performanceId)).willReturn(Optional.of(
                new Performance(performanceId, "공연", Instant.parse("2026-11-01T10:00:00Z"))));
    }

    private void assertInvalidInput(ThrowingCallable executable) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(executable)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    private Seat seat(Long id, Integer seatNumber) {
        return new Seat(id, 7L, "A", "1", seatNumber, CREATED_AT);
    }
}
