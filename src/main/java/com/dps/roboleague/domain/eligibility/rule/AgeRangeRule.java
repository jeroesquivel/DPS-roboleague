package com.dps.roboleague.domain.eligibility.rule;

import com.dps.roboleague.domain.eligibility.EligibilityRequest;
import com.dps.roboleague.domain.eligibility.EligibilityRule;
import com.dps.roboleague.domain.eligibility.EligibilityViolation;
import com.dps.roboleague.domain.shared.AgeRange;
import com.dps.roboleague.domain.team.Member;
import java.util.List;

public final class AgeRangeRule implements EligibilityRule {

    public static final String CODE = "AGE_RANGE";

    @Override
    public List<EligibilityViolation> evaluate(EligibilityRequest request) {
        AgeRange range = request.category().ageRange();
        return request.registration().competitors().stream()
                .filter(member -> !range.includes(member.ageOn(request.referenceDate())))
                .map(member -> violationFor(member, request))
                .toList();
    }

    private EligibilityViolation violationFor(Member member, EligibilityRequest request) {
        AgeRange range = request.category().ageRange();
        return new EligibilityViolation(CODE, "%s is %d years old and category %s admits ages %d to %d"
                .formatted(member.fullName(), member.ageOn(request.referenceDate()), request.category().name(),
                        range.minimumYears(), range.maximumYears()));
    }
}
