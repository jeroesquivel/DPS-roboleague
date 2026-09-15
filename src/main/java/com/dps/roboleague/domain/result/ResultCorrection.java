package com.dps.roboleague.domain.result;

import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.DomainException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ResultCorrection(Instant appliedAt, String actor, String reason, MeasurementSet measurements,
        List<IncidentReport> incidents, Optional<AppealId> sourceAppeal) {

    public ResultCorrection {
        Objects.requireNonNull(appliedAt, "correction timestamp is required");
        Objects.requireNonNull(measurements, "corrected measurements are required");
        Objects.requireNonNull(sourceAppeal, "source appeal is required");
        if (actor == null || actor.isBlank() || reason == null || reason.isBlank()) {
            throw new DomainException("a correction requires an actor and a reason");
        }
        incidents = List.copyOf(incidents);
    }

    public static ResultCorrection fromAppeal(AppealId appealId, Instant appliedAt, String actor, String reason,
            MeasurementSet measurements, List<IncidentReport> incidents) {
        return new ResultCorrection(appliedAt, actor, reason, measurements, incidents, Optional.of(appealId));
    }
}
