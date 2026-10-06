package kr.noco.qticket.infra.persistence.seat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatJpaRepository extends JpaRepository<SeatEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE seat SET sale_status = 'SOLD' "
            + "WHERE id = :seatId AND performance_id = :performanceId "
            + "AND sale_status = 'AVAILABLE'", nativeQuery = true)
    int markSoldIfAvailable(@Param("performanceId") Long performanceId,
                            @Param("seatId") Long seatId);
}
