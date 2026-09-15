package com.dps.roboleague.domain.schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ScheduleConflictDetector {

    public static final String ARENA_BUSY = "ARENA_BUSY";
    public static final String TEAM_BUSY = "TEAM_BUSY";
    public static final String JUDGE_BUSY = "JUDGE_BUSY";

    public List<ScheduleConflict> detect(Collection<Heat> scheduled, Heat candidate) {
        List<ScheduleConflict> conflicts = new ArrayList<>();
        for (Heat heat : scheduled) {
            if (heat.id().equals(candidate.id()) || !heat.slot().overlaps(candidate.slot())) {
                continue;
            }
            if (heat.arenaId().equals(candidate.arenaId())) {
                conflicts.add(new ScheduleConflict(ARENA_BUSY,
                        "arena " + candidate.arenaId().value() + " is taken by heat " + heat.id().value()));
            }
            if (heat.teamId().equals(candidate.teamId())) {
                conflicts.add(new ScheduleConflict(TEAM_BUSY,
                        "team " + candidate.teamId().value() + " is already running in heat " + heat.id().value()));
            }
            if (heat.sharesJudgeWith(candidate)) {
                conflicts.add(new ScheduleConflict(JUDGE_BUSY,
                        "a judge is already assigned to heat " + heat.id().value()));
            }
        }
        return List.copyOf(conflicts);
    }
}
