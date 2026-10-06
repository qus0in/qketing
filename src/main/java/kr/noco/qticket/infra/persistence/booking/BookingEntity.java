package kr.noco.qticket.infra.persistence.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(name = "booking", uniqueConstraints = {
        @UniqueConstraint(name = "uk_booking_idempotency_key",
                columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_booking_reference",
                columnNames = {"id", "performance_id", "seat_id"})
})
@Getter
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "performance_id", nullable = false)
    private Long performanceId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected BookingEntity() {
    }

    private BookingEntity(String idempotencyKey, Long performanceId, Long seatId) {
        this.idempotencyKey = idempotencyKey;
        this.performanceId = performanceId;
        this.seatId = seatId;
    }

    public static BookingEntity of(String idempotencyKey, Long performanceId, Long seatId) {
        return new BookingEntity(idempotencyKey, performanceId, seatId);
    }
}
