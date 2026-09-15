package com.dps.roboleague.domain.schedule;

import com.dps.roboleague.domain.shared.ArenaId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.HeatId;
import com.dps.roboleague.domain.shared.JudgeId;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.TeamId;
import java.util.Objects;
import java.util.Set;

public record Heat(HeatId id, RoundId roundId, TeamId teamId, ArenaId arenaId, TimeSlot slot, Set<JudgeId> judges) {

    public Heat {
        Objects.requireNonNull(id, "heat id is required");
        Objects.requireNonNull(roundId, "round id is required");
        Objects.requireNonNull(teamId, "team id is required");
        Objects.requireNonNull(arenaId, "arena id is required");
        Objects.requireNonNull(slot, "time slot is required");
        judges = Set.copyOf(judges);
        if (judges.isEmpty()) {
            throw new DomainException("heat " + id.value() + " requires at least one judge");
        }
    }

    public boolean sharesJudgeWith(Heat other) {
        return judges.stream().anyMatch(other.judges::contains);
    }
}
