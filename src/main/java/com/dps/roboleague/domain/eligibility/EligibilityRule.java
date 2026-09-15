package com.dps.roboleague.domain.eligibility;

import java.util.List;

public interface EligibilityRule {

    String code();

    List<EligibilityViolation> evaluate(EligibilityRequest request);
}
