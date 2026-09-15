package com.dps.roboleague.domain.scoring;

import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import java.util.Objects;

public record ScoreContribution(String ruleCode, String explanation, Points points) {

    public ScoreContribution {
        Objects.requireNonNull(points, "points are required");
        if (ruleCode == null || ruleCode.isBlank() || explanation == null || explanation.isBlank()) {
            throw new DomainException("a score contribution requires a rule code and an explanation");
        }
    }
}
