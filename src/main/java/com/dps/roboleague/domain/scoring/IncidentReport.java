package com.dps.roboleague.domain.scoring;

import com.dps.roboleague.domain.shared.DomainException;
import java.util.Objects;

public record IncidentReport(PenaltyCode code, int occurrences) {

    public IncidentReport {
        Objects.requireNonNull(code, "penalty code is required");
        if (occurrences < 1) {
            throw new DomainException("an incident requires at least one occurrence");
        }
    }

    public static IncidentReport once(PenaltyCode code) {
        return new IncidentReport(code, 1);
    }
}
