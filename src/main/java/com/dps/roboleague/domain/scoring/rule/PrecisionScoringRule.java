package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record PrecisionScoringRule(MetricKey metric, Points maximumPoints) implements ScoringRule {

    public static final String CODE = "PRECISION";

    public PrecisionScoringRule {
        Objects.requireNonNull(metric, "metric is required");
        Objects.requireNonNull(maximumPoints, "maximum points are required");
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        BigDecimal ratio = context.measurements().require(metric).amount();
        Points earned = maximumPoints.times(ratio);
        String explanation = "precision ratio %s over a maximum of %s points".formatted(ratio.toPlainString(),
                maximumPoints);
        return List.of(new ScoreContribution(CODE, explanation, earned));
    }
}
