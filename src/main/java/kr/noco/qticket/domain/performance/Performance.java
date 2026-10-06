package kr.noco.qticket.domain.performance;

import java.time.Instant;
import java.util.Objects;

public record Performance(Long id, String title, Instant startsAt) {

    public Performance {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("Performance id must be positive");
        }
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new IllegalArgumentException("Performance title is invalid");
        }
        Objects.requireNonNull(startsAt);
    }
}
