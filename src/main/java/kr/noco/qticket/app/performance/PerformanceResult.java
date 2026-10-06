package kr.noco.qticket.app.performance;

import java.time.Instant;
import kr.noco.qticket.domain.performance.Performance;

public record PerformanceResult(Long id, String title, Instant startsAt) {

    public static PerformanceResult from(Performance performance) {
        return new PerformanceResult(performance.id(), performance.title(), performance.startsAt());
    }
}
