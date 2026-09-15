package com.dps.roboleague.domain.scoring;

/**
 * Qué representa una contribución dentro del puntaje, con independencia de qué regla la produjo.
 * Es el concepto que consulta el ranking: así los desempates no dependen de ninguna clase concreta
 * de {@code domain.scoring.rule}.
 */
public enum ContributionKind {

    /** Puntos ganados por el desempeño medido en la corrida. */
    EARNED,

    /** Bonificación otorgada por alcanzar una condición del reglamento. */
    BONUS,

    /** Deducción originada en un incidente registrado durante la corrida. */
    PENALTY
}
