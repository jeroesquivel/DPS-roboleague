package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.scoring.ScoringRuleCode;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record PrecisionScoringRule(MetricKey metric, Points maximumPoints) implements ScoringRule {

    public static final ScoringRuleCode CODE = ScoringRuleCode.of("PRECISION");

    public PrecisionScoringRule {
        Objects.requireNonNull(metric, "metric is required");
        Objects.requireNonNull(maximumPoints, "maximum points are required");
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        return context.measurements().amountOf(metric)
                .map(this::contributionFor)
                .orElseGet(() -> List.of(ScoreContribution.earned(CODE,
                        "no measurement recorded for " + metric.value(), Points.ZERO)));
    }

    private List<ScoreContribution> contributionFor(BigDecimal ratio) {
        Points earned = maximumPoints.times(ratio);
        String explanation = "precision ratio %s over a maximum of %s points".formatted(ratio.toPlainString(),
                maximumPoints);
        return List.of(ScoreContribution.earned(CODE, explanation, earned));
    }
}
