package kr.noco.qticket.infra.persistence.seat;

import java.util.List;
import kr.noco.qticket.app.seat.SeatPersistencePort;
import kr.noco.qticket.domain.seat.Seat;
import org.springframework.stereotype.Repository;

@Repository
public class SeatPersistenceAdapter implements SeatPersistencePort {

    private final SeatQueryRepository queryRepository;
    private final SeatJpaRepository seatRepository;

    public SeatPersistenceAdapter(SeatQueryRepository queryRepository,
                                  SeatJpaRepository seatRepository) {
        this.queryRepository = queryRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    public List<Seat> findByPerformanceId(Long performanceId,
                                          kr.noco.qticket.domain.seat.SeatSort sort,
                                          kr.noco.qticket.domain.seat.SortDirection direction) {
        return queryRepository.findByPerformanceId(performanceId, toPersistenceSort(sort),
                        toPersistenceDirection(direction)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean claimForSale(Long performanceId, Long seatId) {
        return seatRepository.markSoldIfAvailable(performanceId, seatId) == 1;
    }

    private SeatSort toPersistenceSort(kr.noco.qticket.domain.seat.SeatSort sort) {
        return switch (sort) {
            case POSITION -> SeatSort.POSITION;
            case CREATED_AT -> SeatSort.CREATED_AT;
        };
    }

    private SortDirection toPersistenceDirection(
            kr.noco.qticket.domain.seat.SortDirection direction) {
        return switch (direction) {
            case ASC -> SortDirection.ASC;
            case DESC -> SortDirection.DESC;
        };
    }

    private Seat toDomain(SeatEntity entity) {
        return new Seat(entity.getId(), entity.getPerformanceId(), entity.getSection(),
                entity.getRowLabel(), entity.getSeatNumber(), entity.getCreatedAt());
    }
}
