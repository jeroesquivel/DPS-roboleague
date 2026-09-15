package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.PenaltyCode;
import com.dps.roboleague.domain.scoring.PenaltyDefinition;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.scoring.ScoringRuleCode;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PenaltyScoringRule(Map<PenaltyCode, PenaltyDefinition> catalog) implements ScoringRule {

    public static final ScoringRuleCode CODE = ScoringRuleCode.of("PENALTIES");

    public PenaltyScoringRule {
        catalog = Map.copyOf(catalog);
    }

    public static PenaltyScoringRule of(Collection<PenaltyDefinition> definitions) {
        return new PenaltyScoringRule(definitions.stream()
                .collect(Collectors.toUnmodifiableMap(PenaltyDefinition::code, Function.identity())));
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        if (context.incidents().isEmpty()) {
            return List.of(ScoreContribution.penalty(CODE, "no incidents reported", Points.ZERO));
        }
        return context.incidents().stream().map(this::contributionFor).toList();
    }

    private ScoreContribution contributionFor(IncidentReport incident) {
        PenaltyDefinition definition = catalog.get(incident.code());
        if (definition == null) {
            return ScoreContribution.penalty(CODE, "penalty " + incident.code().value()
                    + " is not defined in the rulebook: no deduction applied", Points.ZERO);
        }
        Points deduction = definition.deduction().times(BigDecimal.valueOf(incident.occurrences())).negated();
        String explanation = "%s applied %d time(s)".formatted(definition.description(), incident.occurrences());
        return ScoreContribution.penalty(CODE, explanation, deduction);
    }
}
