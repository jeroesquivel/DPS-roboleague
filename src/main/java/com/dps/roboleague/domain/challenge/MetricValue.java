package com.dps.roboleague.domain.challenge;

import com.dps.roboleague.domain.shared.DomainException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Objects;

public record MetricValue(BigDecimal amount) {

    public static final int SCALE = 3;

    public MetricValue {
        Objects.requireNonNull(amount, "metric amount is required");
        if (amount.signum() < 0) {
            throw new DomainException("metric amount cannot be negative");
        }
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static MetricValue of(BigDecimal amount) {
        return new MetricValue(amount);
    }

    public static MetricValue of(String amount) {
        return new MetricValue(new BigDecimal(amount));
    }

    public static MetricValue of(long amount) {
        return new MetricValue(BigDecimal.valueOf(amount));
    }

    public static MetricValue ofSeconds(Duration duration) {
        return new MetricValue(new BigDecimal(duration.toMillis()).movePointLeft(3));
    }

    public int compareTo(MetricValue other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.stripTrailingZeros().toPlainString();
    }
}
