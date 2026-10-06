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
@Table(name = "ticket", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ticket_booking", columnNames = "booking_id"),
        @UniqueConstraint(name = "uk_ticket_sale",
                columnNames = {"performance_id", "seat_id"})
})
@Getter
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "performance_id", nullable = false)
    private Long performanceId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected TicketEntity() {
    }

    private TicketEntity(Long bookingId, Long performanceId, Long seatId) {
        this.bookingId = bookingId;
        this.performanceId = performanceId;
        this.seatId = seatId;
    }

    public static TicketEntity of(Long bookingId, Long performanceId, Long seatId) {
        return new TicketEntity(bookingId, performanceId, seatId);
    }
}
