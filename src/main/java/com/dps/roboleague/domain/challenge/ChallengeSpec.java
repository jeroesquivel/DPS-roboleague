package com.dps.roboleague.domain.challenge;

import com.dps.roboleague.domain.scoring.ScoreBreakdown;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.DomainException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ChallengeSpec(ChallengeId id, String name, List<MetricDefinition> metrics, ScoringRule scoringRule,
        int maximumAttempts) {

    public ChallengeSpec {
        Objects.requireNonNull(id, "challenge id is required");
        Objects.requireNonNull(scoringRule, "scoring rule is required");
        if (name == null || name.isBlank()) {
            throw new DomainException("challenge requires a name");
        }
        metrics = List.copyOf(metrics);
        if (metrics.isEmpty()) {
            throw new DomainException("challenge " + name + " requires at least one metric");
        }
        if (maximumAttempts < 1) {
            throw new DomainException("challenge " + name + " requires at least one attempt");
        }
    }

    public void validate(MeasurementSet measurements) {
        measurements.keys().stream()
                .filter(key -> definitionOf(key).isEmpty())
                .findFirst()
                .ifPresent(unknown -> {
                    throw new DomainException("metric " + unknown.value() + " is not defined for challenge " + name);
                });
        metrics.forEach(definition -> validateAgainst(definition, measurements));
    }

    public ScoreBreakdown score(ScoringContext context) {
        return scoringRule.breakdownFor(context);
    }

    public void requireAttemptWithinLimit(int attemptNumber) {
        if (attemptNumber < 1 || attemptNumber > maximumAttempts) {
            throw new DomainException("challenge " + name + " allows " + maximumAttempts + " attempts and attempt "
                    + attemptNumber + " is out of range");
        }
    }

    private void validateAgainst(MetricDefinition definition, MeasurementSet measurements) {
        measurements.find(definition.key()).ifPresentOrElse(definition::validate, () -> {
            if (definition.required()) {
                throw new DomainException(
                        "challenge " + name + " requires a measurement for " + definition.key().value());
            }
        });
    }

    private Optional<MetricDefinition> definitionOf(MetricKey key) {
        return metrics.stream().filter(metric -> metric.key().equals(key)).findFirst();
    }
}
