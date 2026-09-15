package com.dps.roboleague.domain.schedule;

import com.dps.roboleague.domain.shared.DomainException;

public record ScheduleConflict(String code, String detail) {

    public ScheduleConflict {
        if (code == null || code.isBlank() || detail == null || detail.isBlank()) {
            throw new DomainException("a schedule conflict requires a code and a detail");
        }
    }
}
