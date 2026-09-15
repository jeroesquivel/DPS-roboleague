package com.dps.roboleague.domain.scoring;

import com.dps.roboleague.domain.shared.Points;
import java.util.List;

public record ScoreBreakdown(List<ScoreContribution> contributions) {

    public ScoreBreakdown {
        contributions = List.copyOf(contributions);
    }

    public static ScoreBreakdown empty() {
        return new ScoreBreakdown(List.of());
    }

    public Points total() {
        return contributions.stream().map(ScoreContribution::points).reduce(Points.ZERO, Points::plus);
    }

    public Points totalFor(String ruleCode) {
        return contributions.stream()
                .filter(contribution -> contribution.ruleCode().equals(ruleCode))
                .map(ScoreContribution::points)
                .reduce(Points.ZERO, Points::plus);
    }
}
