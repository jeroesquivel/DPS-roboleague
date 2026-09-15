package com.dps.roboleague.domain.challenge;

import com.dps.roboleague.domain.shared.DomainException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record MeasurementSet(Map<MetricKey, MetricValue> values) {

    public MeasurementSet {
        values = Map.copyOf(values);
    }

    public static MeasurementSet empty() {
        return new MeasurementSet(Map.of());
    }

    public MeasurementSet with(MetricKey key, MetricValue value) {
        Map<MetricKey, MetricValue> merged = new LinkedHashMap<>(values);
        merged.put(key, value);
        return new MeasurementSet(merged);
    }

    public Optional<MetricValue> find(MetricKey key) {
        return Optional.ofNullable(values.get(key));
    }

    /**
     * El valor medido para esa métrica, si fue registrado. Es lo que consultan las reglas de
     * puntaje: una regla nunca exige un dato, explica su ausencia.
     */
    public Optional<BigDecimal> amountOf(MetricKey key) {
        return find(key).map(MetricValue::amount);
    }

    public MetricValue require(MetricKey key) {
        return find(key).orElseThrow(() -> new DomainException("missing measurement for metric " + key.value()));
    }

    public Set<MetricKey> keys() {
        return values.keySet();
    }
}
