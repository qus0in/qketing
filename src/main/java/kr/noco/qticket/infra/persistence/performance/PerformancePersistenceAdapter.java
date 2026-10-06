package kr.noco.qticket.infra.persistence.performance;

import java.util.Optional;
import kr.noco.qticket.app.performance.PerformancePersistencePort;
import kr.noco.qticket.domain.performance.Performance;
import org.springframework.stereotype.Repository;

@Repository
public class PerformancePersistenceAdapter implements PerformancePersistencePort {

    private final PerformanceJpaRepository repository;

    public PerformancePersistenceAdapter(PerformanceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Performance> findById(Long performanceId) {
        return repository.findById(performanceId).map(this::toDomain);
    }

    private Performance toDomain(PerformanceEntity entity) {
        return new Performance(entity.getId(), entity.getTitle(), entity.getStartsAt());
    }
}
