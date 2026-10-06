package kr.noco.qticket.app.performance;

import java.util.Optional;
import kr.noco.qticket.domain.performance.Performance;

public interface PerformancePersistencePort {

    Optional<Performance> findById(Long performanceId);
}
