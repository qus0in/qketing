package kr.noco.qticket.infra.persistence.performance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(name = "performance")
@Getter
public class PerformanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected PerformanceEntity() {
    }

    private PerformanceEntity(String title, Instant startsAt) {
        this.title = title;
        this.startsAt = startsAt;
    }

    public static PerformanceEntity of(String title, Instant startsAt) {
        return new PerformanceEntity(title, startsAt);
    }
}
