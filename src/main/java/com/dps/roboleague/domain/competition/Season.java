package com.dps.roboleague.domain.competition;

import com.dps.roboleague.domain.shared.DateRange;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.SeasonId;
import java.util.Objects;

public record Season(SeasonId id, String name, int year, DateRange period) {

    public Season {
        Objects.requireNonNull(id, "season id is required");
        Objects.requireNonNull(period, "season period is required");
        if (name == null || name.isBlank()) {
            throw new DomainException("season requires a name");
        }
        if (period.start().getYear() != year) {
            throw new DomainException("season period must start within the season year " + year);
        }
    }

    public void requireCompetitionPeriodInside(DateRange competitionPeriod) {
        if (!period.contains(competitionPeriod.start()) || !period.contains(competitionPeriod.end())) {
            throw new DomainException("competition period " + competitionPeriod + " is outside season " + name);
        }
    }
}
