package kr.noco.qticket.infra.persistence.seat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(name = "seat", uniqueConstraints = {
        @UniqueConstraint(name = "uk_seat_position", columnNames = {
                "performance_id", "section", "row_label", "seat_number"
        })
})
@Getter
public class SeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "performance_id", nullable = false)
    private Long performanceId;

    @Column(nullable = false, length = 16)
    private String section;

    @Column(name = "row_label", nullable = false, length = 8)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", nullable = false, length = 16)
    private SaleStatus saleStatus;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected SeatEntity() {
    }

    private SeatEntity(Long performanceId, String section, String rowLabel, Integer seatNumber) {
        this.performanceId = performanceId;
        this.section = section;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.saleStatus = SaleStatus.AVAILABLE;
    }

    public static SeatEntity of(Long performanceId, String section, String rowLabel,
                                Integer seatNumber) {
        return new SeatEntity(performanceId, section, rowLabel, seatNumber);
    }

    public enum SaleStatus {
        AVAILABLE,
        SOLD
    }
}
