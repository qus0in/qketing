package kr.noco.qticket.infra.persistence.seat;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** snapshot 전용 원본 조회 repository(기존 repository는 수정하지 않는다). */
public interface SeatSnapshotJpaRepository extends JpaRepository<SeatEntity, Long> {

    @Query("select s from SeatEntity s where s.performanceId = :performanceId order by s.id")
    List<SeatEntity> findSeatsByPerformanceId(@Param("performanceId") Long performanceId);
}
