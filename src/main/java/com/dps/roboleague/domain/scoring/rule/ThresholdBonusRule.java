package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ThresholdBonusRule(MetricKey metric, Comparison comparison, BigDecimal threshold, Points bonus)
        implements ScoringRule {

    public static final String CODE = "BONUS";

    public enum Comparison {
        AT_LEAST,
        AT_MOST
    }

    public ThresholdBonusRule {
        Objects.requireNonNull(metric, "metric is required");
        Objects.requireNonNull(comparison, "comparison is required");
        Objects.requireNonNull(threshold, "threshold is required");
        Objects.requireNonNull(bonus, "bonus is required");
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        BigDecimal measured = context.measurements().require(metric).amount();
        boolean granted = switch (comparison) {
            case AT_LEAST -> measured.compareTo(threshold) >= 0;
            case AT_MOST -> measured.compareTo(threshold) <= 0;
        };
        String explanation = "%s %s %s: bonus %s".formatted(metric.value(), comparison, threshold.toPlainString(),
                granted ? "granted" : "not granted");
        return List.of(new ScoreContribution(CODE, explanation, granted ? bonus : Points.ZERO));
    }
}
