package com.dps.roboleague.domain.scoring;

import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import java.util.Objects;

public record PenaltyDefinition(PenaltyCode code, String description, Points deduction) {

    public PenaltyDefinition {
        Objects.requireNonNull(code, "penalty code is required");
        Objects.requireNonNull(deduction, "penalty deduction is required");
        if (description == null || description.isBlank()) {
            throw new DomainException("penalty " + code.value() + " requires a description");
        }
        if (deduction.isNegative()) {
            throw new DomainException("penalty deduction is expressed as a positive amount");
        }
    }
}
