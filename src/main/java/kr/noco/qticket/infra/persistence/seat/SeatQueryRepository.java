package kr.noco.qticket.infra.persistence.seat;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import kr.noco.qticket.app.error.BusinessException;
import kr.noco.qticket.app.error.ErrorCode;
import org.springframework.stereotype.Repository;

@Repository
public class SeatQueryRepository {

    private final JPAQueryFactory queryFactory;

    public SeatQueryRepository(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<SeatEntity> findByPerformanceId(Long performanceId, SeatSort sort,
                                                SortDirection direction) {
        QSeatEntity seat = QSeatEntity.seatEntity;
        OrderSpecifier<?>[] order = orderBy(seat, sort, direction);
        return queryFactory.selectFrom(seat)
                .where(seat.performanceId.eq(performanceId))
                .orderBy(order)
                .fetch();
    }

    private OrderSpecifier<?>[] orderBy(QSeatEntity seat, SeatSort sort,
                                        SortDirection direction) {
        if (sort == null || direction == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return switch (sort) {
            case POSITION -> positionOrder(seat, direction);
            case CREATED_AT -> createdAtOrder(seat, direction);
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
    }

    private OrderSpecifier<?>[] positionOrder(QSeatEntity seat, SortDirection direction) {
        if (direction == SortDirection.ASC) {
            return new OrderSpecifier<?>[]{seat.section.asc(), seat.rowLabel.asc(),
                    seat.seatNumber.asc()};
        }
        return new OrderSpecifier<?>[]{seat.section.desc(), seat.rowLabel.desc(),
                seat.seatNumber.desc()};
    }

    private OrderSpecifier<?>[] createdAtOrder(QSeatEntity seat, SortDirection direction) {
        if (direction == SortDirection.ASC) {
            return new OrderSpecifier<?>[]{seat.createdAt.asc()};
        }
        return new OrderSpecifier<?>[]{seat.createdAt.desc()};
    }
}
