package com.dps.roboleague.domain.schedule;

import com.dps.roboleague.domain.shared.DomainException;
import java.util.Objects;

public record ScheduleConflict(ScheduleConflictType type, String detail) {

    public ScheduleConflict {
        Objects.requireNonNull(type, "conflict type is required");
        if (detail == null || detail.isBlank()) {
            throw new DomainException("a schedule conflict requires a detail");
        }
    }
}
