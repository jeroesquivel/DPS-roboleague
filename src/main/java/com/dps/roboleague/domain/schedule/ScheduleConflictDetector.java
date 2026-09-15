package com.dps.roboleague.domain.schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Detectar que una pista, un equipo o un juez ya están ocupados exige mirar turnos de varias rondas,
 * así que la regla no pertenece a ninguna entidad.
 */
public final class ScheduleConflictDetector {

    public List<ScheduleConflict> detect(Collection<Heat> scheduled, Heat candidate) {
        List<ScheduleConflict> conflicts = new ArrayList<>();
        for (Heat heat : scheduled) {
            if (heat.id().equals(candidate.id()) || !heat.slot().overlaps(candidate.slot())) {
                continue;
            }
            if (heat.arenaId().equals(candidate.arenaId())) {
                conflicts.add(new ScheduleConflict(ScheduleConflictType.ARENA_BUSY,
                        "arena " + candidate.arenaId().value() + " is taken by heat " + heat.id().value()));
            }
            if (heat.teamId().equals(candidate.teamId())) {
                conflicts.add(new ScheduleConflict(ScheduleConflictType.TEAM_BUSY,
                        "team " + candidate.teamId().value() + " is already running in heat " + heat.id().value()));
            }
            if (heat.sharesJudgeWith(candidate)) {
                conflicts.add(new ScheduleConflict(ScheduleConflictType.JUDGE_BUSY,
                        "a judge is already assigned to heat " + heat.id().value()));
            }
        }
        return List.copyOf(conflicts);
    }
}
