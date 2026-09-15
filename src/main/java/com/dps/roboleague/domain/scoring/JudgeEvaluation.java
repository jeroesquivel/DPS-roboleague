package com.dps.roboleague.domain.scoring;

import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.JudgeId;
import com.dps.roboleague.domain.shared.Points;
import java.util.Objects;

public record JudgeEvaluation(JudgeId judge, MetricKey criterion, Points score) {

    public JudgeEvaluation {
        Objects.requireNonNull(judge, "judge is required");
        Objects.requireNonNull(criterion, "criterion is required");
        Objects.requireNonNull(score, "score is required");
        if (score.isNegative()) {
            throw new DomainException("a judge score cannot be negative");
        }
    }
}
