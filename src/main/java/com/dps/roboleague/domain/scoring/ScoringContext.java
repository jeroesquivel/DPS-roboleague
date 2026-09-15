package com.dps.roboleague.domain.scoring;

import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricKey;
import java.util.List;
import java.util.Objects;

public record ScoringContext(MeasurementSet measurements, List<JudgeEvaluation> evaluations,
        List<IncidentReport> incidents) {

    public ScoringContext {
        Objects.requireNonNull(measurements, "measurements are required");
        evaluations = List.copyOf(evaluations);
        incidents = List.copyOf(incidents);
    }

    public static ScoringContext of(MeasurementSet measurements) {
        return new ScoringContext(measurements, List.of(), List.of());
    }

    public List<JudgeEvaluation> evaluationsFor(MetricKey criterion) {
        return evaluations.stream().filter(evaluation -> evaluation.criterion().equals(criterion)).toList();
    }
}
