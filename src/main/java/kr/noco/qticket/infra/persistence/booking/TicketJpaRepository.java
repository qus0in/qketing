package kr.noco.qticket.infra.persistence.booking;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, Long> {

    Optional<TicketEntity> findByBookingId(Long bookingId);
}
