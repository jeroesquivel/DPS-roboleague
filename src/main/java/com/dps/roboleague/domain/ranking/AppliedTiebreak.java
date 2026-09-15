package com.dps.roboleague.domain.ranking;

import com.dps.roboleague.domain.shared.DomainException;

/**
 * Qué criterio resolvió un empate. Lleva el código, para ordenar o filtrar por máquina, y la
 * descripción, que es lo que se le muestra a un juez o a un equipo.
 */
public record AppliedTiebreak(String code, String description) {

    public AppliedTiebreak {
        if (code == null || code.isBlank() || description == null || description.isBlank()) {
            throw new DomainException("an applied tiebreak requires a code and a description");
        }
    }

    public static AppliedTiebreak of(TiebreakRule rule) {
        return new AppliedTiebreak(rule.code(), rule.description());
    }
}
