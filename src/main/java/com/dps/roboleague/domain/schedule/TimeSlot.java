package com.dps.roboleague.domain.schedule;

import com.dps.roboleague.domain.shared.DomainException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record TimeSlot(LocalDateTime start, Duration duration) {

    public TimeSlot {
        Objects.requireNonNull(start, "start is required");
        Objects.requireNonNull(duration, "duration is required");
        if (duration.isZero() || duration.isNegative()) {
            throw new DomainException("a time slot requires a positive duration");
        }
    }

    public LocalDateTime end() {
        return start.plus(duration);
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end()) && other.start.isBefore(end());
    }
}
