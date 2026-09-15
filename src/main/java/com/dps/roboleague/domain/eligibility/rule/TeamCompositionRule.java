package com.dps.roboleague.domain.eligibility.rule;

import com.dps.roboleague.domain.eligibility.EligibilityRequest;
import com.dps.roboleague.domain.eligibility.EligibilityRule;
import com.dps.roboleague.domain.eligibility.EligibilityViolation;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.team.Member;
import com.dps.roboleague.domain.team.MemberRole;
import java.util.ArrayList;
import java.util.List;

public final class TeamCompositionRule implements EligibilityRule {

    public static final String CODE = "TEAM_COMPOSITION";

    private final int minimumCompetitors;
    private final int maximumCompetitors;
    private final int coachRequiredUnderAge;

    public TeamCompositionRule(int minimumCompetitors, int maximumCompetitors, int coachRequiredUnderAge) {
        if (minimumCompetitors < 1 || maximumCompetitors < minimumCompetitors) {
            throw new DomainException("invalid competitor bounds for the team composition rule");
        }
        this.minimumCompetitors = minimumCompetitors;
        this.maximumCompetitors = maximumCompetitors;
        this.coachRequiredUnderAge = coachRequiredUnderAge;
    }

    @Override
    public List<EligibilityViolation> evaluate(EligibilityRequest request) {
        List<EligibilityViolation> violations = new ArrayList<>();
        List<Member> competitors = request.registration().competitors();
        if (competitors.size() < minimumCompetitors || competitors.size() > maximumCompetitors) {
            violations.add(new EligibilityViolation(CODE, "team has %d competitors and the category admits %d to %d"
                    .formatted(competitors.size(), minimumCompetitors, maximumCompetitors)));
        }
        if (hasMinors(request) && !hasCoach(request)) {
            violations.add(new EligibilityViolation(CODE,
                    "teams with competitors under %d require a coach".formatted(coachRequiredUnderAge)));
        }
        return List.copyOf(violations);
    }

    private boolean hasMinors(EligibilityRequest request) {
        return request.registration().competitors().stream()
                .anyMatch(member -> member.ageOn(request.referenceDate()) < coachRequiredUnderAge);
    }

    private boolean hasCoach(EligibilityRequest request) {
        return request.registration().members().stream().anyMatch(member -> member.role() == MemberRole.COACH);
    }
}
