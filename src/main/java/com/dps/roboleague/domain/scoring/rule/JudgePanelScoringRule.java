package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.scoring.JudgeEvaluation;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.scoring.ScoringRuleCode;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public record JudgePanelScoringRule(MetricKey criterion, BigDecimal weight) implements ScoringRule {

    public static final ScoringRuleCode CODE = ScoringRuleCode.of("JUDGES");

    public JudgePanelScoringRule {
        Objects.requireNonNull(criterion, "criterion is required");
        Objects.requireNonNull(weight, "weight is required");
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        List<JudgeEvaluation> evaluations = context.evaluationsFor(criterion);
        if (evaluations.isEmpty()) {
            return List.of(ScoreContribution.earned(CODE,
                    "no evaluations recorded for criterion " + criterion.value(), Points.ZERO));
        }
        BigDecimal sum = evaluations.stream()
                .map(evaluation -> evaluation.score().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(evaluations.size()), Points.SCALE, RoundingMode.HALF_UP);
        Points earned = Points.of(average).times(weight);
        String explanation = "average of %d evaluations for %s is %s weighted by %s".formatted(evaluations.size(),
                criterion.value(), average.toPlainString(), weight.toPlainString());
        return List.of(ScoreContribution.earned(CODE, explanation, earned));
    }
}
