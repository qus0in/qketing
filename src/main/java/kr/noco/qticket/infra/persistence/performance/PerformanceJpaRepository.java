package kr.noco.qticket.infra.persistence.performance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceJpaRepository extends JpaRepository<PerformanceEntity, Long> {
}
