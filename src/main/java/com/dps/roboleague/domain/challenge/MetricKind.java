package com.dps.roboleague.domain.challenge;

import java.math.BigDecimal;

public enum MetricKind {

    TIME_SECONDS,
    OBJECTIVE_COUNT,
    PRECISION_RATIO,
    RESOURCE_UNITS,
    JUDGE_CRITERION;

    public boolean accepts(BigDecimal amount) {
        if (amount.signum() < 0) {
            return false;
        }
        return switch (this) {
            case PRECISION_RATIO -> amount.compareTo(BigDecimal.ONE) <= 0;
            case OBJECTIVE_COUNT -> amount.stripTrailingZeros().scale() <= 0;
            default -> true;
        };
    }
}
