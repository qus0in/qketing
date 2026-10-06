package kr.noco.qticket.app.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import kr.noco.qticket.domain.performance.Performance;
import org.junit.jupiter.api.Test;

class FindPerformanceServiceTest {

    private final PerformancePersistencePort performances = mock(PerformancePersistencePort.class);
    private final FindPerformanceService service = new FindPerformanceService(performances);

    @Test
    void givenExistingPerformance_whenFind_thenResultMapped() {
        Performance performance = new Performance(
                7L, "겨울 나그네", Instant.parse("2026-11-01T10:00:00Z"));
        given(performances.findById(7L)).willReturn(Optional.of(performance));

        PerformanceResult result = service.find(new FindPerformanceQuery(7L));

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.title()).isEqualTo("겨울 나그네");
        assertThat(result.startsAt()).isEqualTo(performance.startsAt());
        verify(performances).findById(7L);
    }

    @Test
    void givenUnknownPerformance_whenFind_thenNotFound() {
        given(performances.findById(99L)).willReturn(Optional.empty());

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.find(new FindPerformanceQuery(99L)))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void givenNullOrNonPositiveId_whenQueryCreated_thenInvalidInput() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> new FindPerformanceQuery(null))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> new FindPerformanceQuery(0L))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
