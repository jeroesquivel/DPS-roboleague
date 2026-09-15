package com.dps.roboleague.domain.scoring;

import com.dps.roboleague.domain.shared.Points;
import java.util.List;

public record ScoreBreakdown(List<ScoreContribution> contributions) {

    public ScoreBreakdown {
        contributions = List.copyOf(contributions);
    }

    public Points total() {
        return sumOf(contributions.stream().toList());
    }

    /** Lo aportado por las contribuciones de una naturaleza dada, sin saber qué regla las produjo. */
    public Points totalOf(ContributionKind kind) {
        return sumOf(contributions.stream().filter(contribution -> contribution.kind() == kind).toList());
    }

    public Points totalFor(ScoringRuleCode ruleCode) {
        return sumOf(contributions.stream()
                .filter(contribution -> contribution.ruleCode().equals(ruleCode))
                .toList());
    }

    private Points sumOf(List<ScoreContribution> selected) {
        return selected.stream().map(ScoreContribution::points).reduce(Points.ZERO, Points::plus);
    }
}
