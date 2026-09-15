package com.dps.roboleague.domain.schedule;

public enum ScheduleConflictType {

    /** La pista ya está ocupada por otro turno que se superpone. */
    ARENA_BUSY,

    /** El equipo ya está corriendo en otro turno que se superpone. */
    TEAM_BUSY,

    /** Un juez ya está asignado a otro turno que se superpone. */
    JUDGE_BUSY
}
