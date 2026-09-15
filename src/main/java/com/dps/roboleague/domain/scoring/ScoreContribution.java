package com.dps.roboleague.domain.scoring;

import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import java.util.Objects;

public record ScoreContribution(ScoringRuleCode ruleCode, ContributionKind kind, String explanation,
        Points points) {

    public ScoreContribution {
        Objects.requireNonNull(ruleCode, "rule code is required");
        Objects.requireNonNull(kind, "contribution kind is required");
        Objects.requireNonNull(points, "points are required");
        if (explanation == null || explanation.isBlank()) {
            throw new DomainException("a score contribution requires an explanation");
        }
    }

    public static ScoreContribution earned(ScoringRuleCode ruleCode, String explanation, Points points) {
        return new ScoreContribution(ruleCode, ContributionKind.EARNED, explanation, points);
    }

    public static ScoreContribution bonus(ScoringRuleCode ruleCode, String explanation, Points points) {
        return new ScoreContribution(ruleCode, ContributionKind.BONUS, explanation, points);
    }

    public static ScoreContribution penalty(ScoringRuleCode ruleCode, String explanation, Points points) {
        return new ScoreContribution(ruleCode, ContributionKind.PENALTY, explanation, points);
    }
}
