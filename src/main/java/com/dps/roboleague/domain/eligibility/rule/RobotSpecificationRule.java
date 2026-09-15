package com.dps.roboleague.domain.eligibility.rule;

import com.dps.roboleague.domain.eligibility.EligibilityRequest;
import com.dps.roboleague.domain.eligibility.EligibilityRule;
import com.dps.roboleague.domain.eligibility.EligibilityViolation;
import com.dps.roboleague.domain.team.Dimensions;
import com.dps.roboleague.domain.team.Robot;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RobotSpecificationRule implements EligibilityRule {

    public static final String CODE = "ROBOT_SPECIFICATION";

    private final BigDecimal maximumWeightKg;
    private final Dimensions maximumDimensions;

    public RobotSpecificationRule(BigDecimal maximumWeightKg, Dimensions maximumDimensions) {
        this.maximumWeightKg = Objects.requireNonNull(maximumWeightKg, "maximum weight is required");
        this.maximumDimensions = Objects.requireNonNull(maximumDimensions, "maximum dimensions are required");
    }

    @Override
    public List<EligibilityViolation> evaluate(EligibilityRequest request) {
        List<EligibilityViolation> violations = new ArrayList<>();
        Robot robot = request.registration().robot();
        if (robot.weightKg().compareTo(maximumWeightKg) > 0) {
            violations.add(new EligibilityViolation(CODE, "robot weighs %s kg and the limit is %s kg"
                    .formatted(robot.weightKg().toPlainString(), maximumWeightKg.toPlainString())));
        }
        if (!robot.dimensions().fitsWithin(maximumDimensions)) {
            violations.add(new EligibilityViolation(CODE, "robot exceeds the allowed dimensions"));
        }
        return List.copyOf(violations);
    }
}
