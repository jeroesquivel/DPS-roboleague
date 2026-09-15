package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ResourceScoringRule(MetricKey metric, BigDecimal allowance, Points pointsPerUnitOver)
        implements ScoringRule {

    public static final String CODE = "RESOURCE";

    public ResourceScoringRule {
        Objects.requireNonNull(metric, "metric is required");
        Objects.requireNonNull(allowance, "allowance is required");
        Objects.requireNonNull(pointsPerUnitOver, "points per unit over the allowance are required");
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        BigDecimal consumed = context.measurements().require(metric).amount();
        BigDecimal excess = consumed.subtract(allowance);
        if (excess.signum() <= 0) {
            return List.of(new ScoreContribution(CODE,
                    "consumption %s within the allowance of %s".formatted(consumed.toPlainString(),
                            allowance.toPlainString()),
                    Points.ZERO));
        }
        Points deduction = pointsPerUnitOver.times(excess).negated();
        String explanation = "consumption %s exceeds the allowance of %s by %s"
                .formatted(consumed.toPlainString(), allowance.toPlainString(), excess.toPlainString());
        return List.of(new ScoreContribution(CODE, explanation, deduction));
    }
}
