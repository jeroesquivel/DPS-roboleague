package com.dps.roboleague.domain.eligibility;

import java.util.List;

public record EligibilityPolicy(List<EligibilityRule> rules) implements EligibilityRule {

    public EligibilityPolicy {
        rules = List.copyOf(rules);
    }

    public static EligibilityPolicy of(EligibilityRule... rules) {
        return new EligibilityPolicy(List.of(rules));
    }

    @Override
    public List<EligibilityViolation> evaluate(EligibilityRequest request) {
        return rules.stream().flatMap(rule -> rule.evaluate(request).stream()).toList();
    }

    public EligibilityVerdict verdictFor(EligibilityRequest request) {
        return new EligibilityVerdict(evaluate(request));
    }
}
