package com.dps.roboleague.domain.scoring;

import com.dps.roboleague.domain.shared.DomainException;
import java.util.Locale;

public record PenaltyCode(String value) {

    public PenaltyCode {
        if (value == null || value.isBlank()) {
            throw new DomainException("penalty code requires a non blank value");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
    }

    public static PenaltyCode of(String value) {
        return new PenaltyCode(value);
    }
}
