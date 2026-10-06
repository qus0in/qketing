package kr.noco.qticket.domain.seat;

import java.time.Instant;
import java.util.Objects;

public record Seat(Long id, Long performanceId, String section, String rowLabel,
                   Integer seatNumber, Instant createdAt) {

    public Seat {
        if (id == null || id < 1 || performanceId == null || performanceId < 1) {
            throw new IllegalArgumentException("Seat identifiers must be positive");
        }
        if (section == null || section.isBlank() || section.length() > 16
                || rowLabel == null || rowLabel.isBlank() || rowLabel.length() > 8) {
            throw new IllegalArgumentException("Seat position is invalid");
        }
        if (seatNumber == null || seatNumber < 1) {
            throw new IllegalArgumentException("Seat number must be positive");
        }
        Objects.requireNonNull(createdAt);
    }
}
