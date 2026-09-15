package com.dps.roboleague.domain.ranking;

import java.util.Comparator;

public interface TiebreakRule extends Comparator<TeamScoreSummary> {

    /** Identificador estable del criterio, para registrar qué regla resolvió un empate. */
    String code();

    /** Cómo se lee el criterio en la tabla de posiciones. */
    String description();
}
