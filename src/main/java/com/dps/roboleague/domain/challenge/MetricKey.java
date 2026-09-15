package com.dps.roboleague.domain.challenge;

import com.dps.roboleague.domain.shared.DomainException;
import java.util.Locale;

public record MetricKey(String value) {

    public MetricKey {
        if (value == null || value.isBlank()) {
            throw new DomainException("metric key requires a non blank value");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
    }

    public static MetricKey of(String value) {
        return new MetricKey(value);
    }
}
