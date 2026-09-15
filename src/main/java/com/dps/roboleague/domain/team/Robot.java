package com.dps.roboleague.domain.team;

import com.dps.roboleague.domain.competition.RobotClass;
import com.dps.roboleague.domain.shared.DomainException;
import java.math.BigDecimal;
import java.util.Objects;

public record Robot(String name, RobotClass robotClass, BigDecimal weightKg, Dimensions dimensions) {

    public Robot {
        Objects.requireNonNull(robotClass, "robot class is required");
        Objects.requireNonNull(dimensions, "robot dimensions are required");
        Objects.requireNonNull(weightKg, "robot weight is required");
        if (name == null || name.isBlank()) {
            throw new DomainException("robot requires a name");
        }
        if (weightKg.signum() <= 0) {
            throw new DomainException("robot weight must be positive");
        }
    }
}
