package com.dps.roboleague.domain.challenge;

import com.dps.roboleague.domain.shared.DomainException;
import java.util.Objects;

public record MetricDefinition(MetricKey key, MetricKind kind, String unit, boolean required) {

    public MetricDefinition {
        Objects.requireNonNull(key, "metric key is required");
        Objects.requireNonNull(kind, "metric kind is required");
        if (unit == null || unit.isBlank()) {
            throw new DomainException("metric " + key.value() + " requires a unit");
        }
    }

    public static MetricDefinition required(MetricKey key, MetricKind kind, String unit) {
        return new MetricDefinition(key, kind, unit, true);
    }

    public static MetricDefinition optional(MetricKey key, MetricKind kind, String unit) {
        return new MetricDefinition(key, kind, unit, false);
    }

    public void validate(MetricValue value) {
        if (!kind.accepts(value.amount())) {
            throw new DomainException("value " + value + " is not valid for metric " + key.value() + " of kind " + kind);
        }
    }
}
