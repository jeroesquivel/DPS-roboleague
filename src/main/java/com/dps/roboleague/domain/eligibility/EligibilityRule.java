package com.dps.roboleague.domain.eligibility;

import java.util.List;

public interface EligibilityRule {

    List<EligibilityViolation> evaluate(EligibilityRequest request);
}
