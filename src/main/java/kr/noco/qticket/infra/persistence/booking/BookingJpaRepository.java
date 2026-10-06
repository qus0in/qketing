package kr.noco.qticket.infra.persistence.booking;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingJpaRepository extends JpaRepository<BookingEntity, Long> {

    Optional<BookingEntity> findByIdempotencyKey(String idempotencyKey);
}
