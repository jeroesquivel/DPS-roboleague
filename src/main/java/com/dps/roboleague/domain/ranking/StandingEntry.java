package com.dps.roboleague.domain.ranking;

import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.TeamId;
import java.util.List;
import java.util.Objects;

public record StandingEntry(int position, TeamId teamId, Points totalPoints, List<String> appliedTiebreaks) {

    public StandingEntry {
        Objects.requireNonNull(teamId, "team id is required");
        Objects.requireNonNull(totalPoints, "total points are required");
        if (position < 1) {
            throw new DomainException("a standing position must be positive");
        }
        appliedTiebreaks = List.copyOf(appliedTiebreaks);
    }
}
