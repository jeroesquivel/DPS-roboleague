package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.appeal.AppealStatus;
import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.shared.AppealId;
import java.util.List;
import java.util.Optional;

public interface ResolveAppeal {

    AppealStatus execute(Command command);

    record Command(AppealId appealId, boolean accepted, String reviewer, String rationale,
            Optional<Correction> correction, String actor) {
    }

    record Correction(MeasurementSet measurements, List<IncidentReport> incidents) {
    }
}
