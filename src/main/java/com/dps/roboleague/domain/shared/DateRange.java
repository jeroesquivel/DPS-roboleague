package com.dps.roboleague.domain.shared;

import java.time.LocalDate;
import java.util.Objects;

public record DateRange(LocalDate start, LocalDate end) {

    public DateRange {
        Objects.requireNonNull(start, "start date is required");
        Objects.requireNonNull(end, "end date is required");
        if (end.isBefore(start)) {
            throw new DomainException("end date " + end + " cannot be before start date " + start);
        }
    }

    public static DateRange of(LocalDate start, LocalDate end) {
        return new DateRange(start, end);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
