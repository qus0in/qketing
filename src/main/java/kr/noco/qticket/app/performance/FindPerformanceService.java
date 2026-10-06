package kr.noco.qticket.app.performance;

import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindPerformanceService {

    private final PerformancePersistencePort performances;

    public FindPerformanceService(PerformancePersistencePort performances) {
        this.performances = performances;
    }

    @Transactional(readOnly = true)
    public PerformanceResult find(FindPerformanceQuery query) {
        return performances.findById(query.performanceId())
                .map(PerformanceResult::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
