package com.dps.roboleague.domain.eligibility;

import com.dps.roboleague.domain.shared.DomainException;

public record EligibilityViolation(String ruleCode, String reason) {

    public EligibilityViolation {
        if (ruleCode == null || ruleCode.isBlank() || reason == null || reason.isBlank()) {
            throw new DomainException("an eligibility violation requires a rule code and a reason");
        }
    }
}
