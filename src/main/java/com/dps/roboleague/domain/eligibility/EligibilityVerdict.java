package com.dps.roboleague.domain.eligibility;

import java.util.List;

public record EligibilityVerdict(List<EligibilityViolation> violations) {

    public EligibilityVerdict {
        violations = List.copyOf(violations);
    }

    public boolean isEligible() {
        return violations.isEmpty();
    }

    public List<String> reasons() {
        return violations.stream().map(violation -> violation.ruleCode() + ": " + violation.reason()).toList();
    }
}
