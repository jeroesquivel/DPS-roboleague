package com.dps.roboleague.domain.challenge;

import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.PenaltyCode;
import com.dps.roboleague.domain.scoring.PenaltyDefinition;
import com.dps.roboleague.domain.scoring.ScoreBreakdown;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.scoring.rule.PenaltyScoringRule;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.DomainException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Un desafío del reglamento: sus métricas, sus reglas de puntaje y su catálogo de penalizaciones.
 * El catálogo vive acá y no dentro de la regla de penalización porque es lo que permite rechazar un
 * incidente desconocido al capturar el resultado, en lugar de descubrirlo al calcular el puntaje.
 *
 * <p>Las reglas de puntaje son una lista, no una sola regla que a su vez es un compuesto:
 * combinarlas es responsabilidad exclusiva de {@link #score}, que es su único consumidor, así que
 * envolverlas primero en un {@code ScoringRule} compuesto no evitaba ninguna duplicación real
 * (ver DESIGN.md 2.2 y 5.10).
 */
public record ChallengeSpec(ChallengeId id, String name, List<MetricDefinition> metrics,
        List<ScoringRule> scoringRules, List<PenaltyDefinition> penalties, int maximumAttempts) {

    public ChallengeSpec {
        Objects.requireNonNull(id, "challenge id is required");
        if (name == null || name.isBlank()) {
            throw new DomainException("challenge requires a name");
        }
        metrics = List.copyOf(metrics);
        scoringRules = List.copyOf(scoringRules);
        penalties = List.copyOf(penalties);
        if (metrics.isEmpty()) {
            throw new DomainException("challenge " + name + " requires at least one metric");
        }
        if (scoringRules.isEmpty()) {
            throw new DomainException("challenge " + name + " requires at least one scoring rule");
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

    /** Un incidente que el reglamento no define se rechaza al capturar, no al puntuar. */
    public void validateIncidents(List<IncidentReport> incidents) {
        Set<PenaltyCode> defined = penalties.stream()
                .map(PenaltyDefinition::code)
                .collect(Collectors.toUnmodifiableSet());
        incidents.stream()
                .map(IncidentReport::code)
                .filter(code -> !defined.contains(code))
                .findFirst()
                .ifPresent(unknown -> {
                    throw new DomainException(
                            "penalty " + unknown.value() + " is not defined for challenge " + name);
                });
    }

    public ScoreBreakdown score(ScoringContext context) {
        return new ScoreBreakdown(Stream.concat(
                scoringRules.stream().flatMap(rule -> rule.apply(context).stream()),
                PenaltyScoringRule.of(penalties).apply(context).stream()).toList());
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
