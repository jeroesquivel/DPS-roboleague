package com.dps.roboleague.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Points(BigDecimal value) implements Comparable<Points> {

    public static final int SCALE = 2;
    public static final Points ZERO = Points.of(BigDecimal.ZERO);

    public Points {
        Objects.requireNonNull(value, "points value is required");
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Points of(BigDecimal value) {
        return new Points(value);
    }

    public static Points of(String value) {
        return new Points(new BigDecimal(value));
    }

    public static Points of(long value) {
        return new Points(BigDecimal.valueOf(value));
    }

    public Points plus(Points other) {
        return new Points(value.add(other.value));
    }

    public Points times(BigDecimal factor) {
        return new Points(value.multiply(factor));
    }

    public Points negated() {
        return new Points(value.negate());
    }

    public Points cappedAt(Points maximum) {
        return compareTo(maximum) > 0 ? maximum : this;
    }

    public boolean isNegative() {
        return value.signum() < 0;
    }

    @Override
    public int compareTo(Points other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
