package com.dps.roboleague.domain.ranking;

import com.dps.roboleague.domain.shared.DomainException;

public record AppliedTiebreak(String code, String description) {

    public AppliedTiebreak {
        if (code == null || code.isBlank() || description == null || description.isBlank()) {
            throw new DomainException("an applied tiebreak requires a code and a description");
        }
    }

    public static AppliedTiebreak of(TiebreakRule rule) {
        return new AppliedTiebreak(rule.code(), rule.description());
    }
}
