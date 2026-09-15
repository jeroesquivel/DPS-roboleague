package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record TimeScoringRule(MetricKey metric, Duration reference, Points pointsPerSecondSaved, Points maximumPoints)
        implements ScoringRule {

    public static final String CODE = "TIME";

    public TimeScoringRule {
        Objects.requireNonNull(metric, "metric is required");
        Objects.requireNonNull(reference, "reference time is required");
        Objects.requireNonNull(pointsPerSecondSaved, "points per second saved are required");
        Objects.requireNonNull(maximumPoints, "maximum points are required");
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        BigDecimal elapsed = context.measurements().require(metric).amount();
        BigDecimal referenceSeconds = BigDecimal.valueOf(reference.toMillis()).movePointLeft(3);
        BigDecimal saved = referenceSeconds.subtract(elapsed);
        Points earned = saved.signum() <= 0 ? Points.ZERO : pointsPerSecondSaved.times(saved).cappedAt(maximumPoints);
        String explanation = "%s s against a reference of %s s".formatted(elapsed.toPlainString(),
                referenceSeconds.toPlainString());
        return List.of(new ScoreContribution(CODE, explanation, earned));
    }
}
