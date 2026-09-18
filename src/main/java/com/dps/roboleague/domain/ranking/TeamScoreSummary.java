package com.dps.roboleague.domain.ranking;

import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.scoring.ContributionKind;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.TeamId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TeamScoreSummary(TeamId teamId, List<ScoredRun> runs) {

    public TeamScoreSummary {
        Objects.requireNonNull(teamId, "team id is required");
        runs = List.copyOf(runs);
    }

    public Points totalPoints() {
        return runs.stream().map(ScoredRun::total).reduce(Points.ZERO, Points::plus);
    }

    public Optional<Points> bestRunPoints() {
        return runs.stream().map(ScoredRun::total).max(Comparator.naturalOrder());
    }

    public Points penaltyPoints() {
        return runs.stream()
                .map(run -> run.breakdown().totalOf(ContributionKind.PENALTY))
                .reduce(Points.ZERO, Points::plus);
    }

    public Optional<MetricValue> lowestMeasurement(MetricKey key) {
        return runs.stream()
                .map(run -> run.measurements().find(key))
                .flatMap(Optional::stream)
                .min(Comparator.comparing(MetricValue::amount));
    }
}
