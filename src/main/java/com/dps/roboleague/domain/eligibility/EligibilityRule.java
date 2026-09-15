package com.dps.roboleague.domain.eligibility;

import java.util.List;

public interface EligibilityRule {

    /** Todas las violaciones que la regla encuentra, no la primera: el equipo corrige de una sola vez. */
    List<EligibilityViolation> evaluate(EligibilityRequest request);
}
