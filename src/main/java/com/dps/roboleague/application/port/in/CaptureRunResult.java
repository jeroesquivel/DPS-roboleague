package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.JudgeEvaluation;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;
import java.util.List;

public interface CaptureRunResult {

    RunId execute(Command command);

    record Command(RoundId roundId, TeamId teamId, int attemptNumber, MeasurementSet measurements,
            List<JudgeEvaluation> evaluations, List<IncidentReport> incidents, String actor) {
    }
}
