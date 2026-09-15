package com.dps.roboleague.domain.scoring;

import java.util.List;

public interface ScoringRule {

    /**
     * Devuelve al menos una contribución explicada. Nunca lanza por datos ausentes: si el dato que
     * la regla necesita no está, devuelve una contribución de cero puntos explicando por qué. La
     * validez de las mediciones y de los incidentes se verifica una sola vez, al capturar el
     * resultado, contra el {@code ChallengeSpec} del reglamento fijado en la corrida.
     */
    List<ScoreContribution> apply(ScoringContext context);

    default ScoreBreakdown breakdownFor(ScoringContext context) {
        return new ScoreBreakdown(apply(context));
    }
}
