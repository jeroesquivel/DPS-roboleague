package com.dps.roboleague.domain.eligibility;

import com.dps.roboleague.domain.competition.Category;
import com.dps.roboleague.domain.team.TeamRegistration;
import java.time.LocalDate;
import java.util.Objects;

public record EligibilityRequest(TeamRegistration registration, Category category, LocalDate referenceDate) {

    public EligibilityRequest {
        Objects.requireNonNull(registration, "registration is required");
        Objects.requireNonNull(category, "category is required");
        Objects.requireNonNull(referenceDate, "reference date is required");
    }
}
