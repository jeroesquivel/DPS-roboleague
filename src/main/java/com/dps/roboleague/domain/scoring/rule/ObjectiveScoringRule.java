package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ObjectiveScoringRule(MetricKey metric, Points pointsPerObjective, int maximumObjectives)
        implements ScoringRule {

    public static final String CODE = "OBJECTIVES";

    public ObjectiveScoringRule {
        Objects.requireNonNull(metric, "metric is required");
        Objects.requireNonNull(pointsPerObjective, "points per objective are required");
        if (maximumObjectives < 1) {
            throw new DomainException("the maximum number of objectives must be positive");
        }
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        BigDecimal reported = context.measurements().require(metric).amount();
        BigDecimal counted = reported.min(BigDecimal.valueOf(maximumObjectives));
        Points earned = pointsPerObjective.times(counted);
        String explanation = "%s of %d objectives at %s points each".formatted(counted.toBigInteger(),
                maximumObjectives, pointsPerObjective);
        return List.of(new ScoreContribution(CODE, explanation, earned));
    }
}
