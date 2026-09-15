package com.dps.roboleague.domain.scoring;

import java.util.List;

public interface ScoringRule {

    String code();

    List<ScoreContribution> apply(ScoringContext context);

    default ScoreBreakdown breakdownFor(ScoringContext context) {
        return new ScoreBreakdown(apply(context));
    }
}
