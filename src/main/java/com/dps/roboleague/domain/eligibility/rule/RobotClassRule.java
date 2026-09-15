package com.dps.roboleague.domain.eligibility.rule;

import com.dps.roboleague.domain.competition.RobotClass;
import com.dps.roboleague.domain.eligibility.EligibilityRequest;
import com.dps.roboleague.domain.eligibility.EligibilityRule;
import com.dps.roboleague.domain.eligibility.EligibilityViolation;
import java.util.List;

public final class RobotClassRule implements EligibilityRule {

    public static final String CODE = "ROBOT_CLASS";

    @Override
    public List<EligibilityViolation> evaluate(EligibilityRequest request) {
        RobotClass declared = request.registration().robot().robotClass();
        RobotClass expected = request.category().robotClass();
        if (declared.equals(expected)) {
            return List.of();
        }
        return List.of(new EligibilityViolation(CODE, "robot class %s does not match category class %s"
                .formatted(declared.code(), expected.code())));
    }
}
