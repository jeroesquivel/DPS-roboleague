package com.dps.roboleague.domain.competition;

import com.dps.roboleague.domain.shared.AgeRange;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.DomainException;
import java.util.Objects;

public record Category(CategoryId id, String name, AgeRange ageRange, RobotClass robotClass) {

    public Category {
        Objects.requireNonNull(id, "category id is required");
        Objects.requireNonNull(ageRange, "age range is required");
        Objects.requireNonNull(robotClass, "robot class is required");
        if (name == null || name.isBlank()) {
            throw new DomainException("category requires a name");
        }
    }
}
