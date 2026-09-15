package com.dps.roboleague.domain.eligibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.support.RescueEditionFixture;
import com.dps.roboleague.domain.competition.Category;
import com.dps.roboleague.domain.eligibility.rule.AgeRangeRule;
import com.dps.roboleague.domain.eligibility.rule.RequiredDocumentsRule;
import com.dps.roboleague.domain.eligibility.rule.RobotClassRule;
import com.dps.roboleague.domain.eligibility.rule.RobotSpecificationRule;
import com.dps.roboleague.domain.eligibility.rule.TeamCompositionRule;
import com.dps.roboleague.domain.shared.AgeRange;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.domain.team.Member;
import com.dps.roboleague.domain.team.Robot;
import com.dps.roboleague.domain.team.TeamDocument;
import com.dps.roboleague.domain.team.TeamRegistration;
import com.dps.roboleague.support.TeamFixtures;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EligibilityPolicyTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 3, 1);

    private final Category junior = new Category(CategoryId.of("CAT-1"), "Junior", AgeRange.between(12, 17),
            TeamFixtures.RESCUE_BOT);
    private final EligibilityPolicy policy = RescueEditionFixture.eligibilityPolicy();

    @Test
    void acceptsATeamThatSatisfiesEveryRule() {
        EligibilityVerdict verdict = evaluate(TeamFixtures.eligibleMembers(), TeamFixtures.eligibleRobot(),
                TeamFixtures.completeDocuments());

        assertTrue(verdict.isEligible());
        assertTrue(verdict.violations().isEmpty());
    }

    @Test
    void reportsCompetitorsOutsideTheAgeRangeOfTheCategory() {
        EligibilityVerdict verdict = evaluate(TeamFixtures.membersWithUnderageCompetitor(),
                TeamFixtures.eligibleRobot(), TeamFixtures.completeDocuments());

        assertFalse(verdict.isEligible());
        assertEquals(List.of(AgeRangeRule.CODE), codesOf(verdict));
    }

    @Test
    void requiresACoachWhenTheTeamHasCompetitorsUnderAge() {
        EligibilityVerdict verdict = evaluate(TeamFixtures.membersWithoutCoach(), TeamFixtures.eligibleRobot(),
                TeamFixtures.completeDocuments());

        assertEquals(List.of(TeamCompositionRule.CODE), codesOf(verdict));
    }

    @Test
    void reportsRobotsThatDoNotMatchTheCategoryClassOrItsLimits() {
        EligibilityVerdict wrongClass = evaluate(TeamFixtures.eligibleMembers(), TeamFixtures.wrongClassRobot(),
                TeamFixtures.completeDocuments());
        EligibilityVerdict overweight = evaluate(TeamFixtures.eligibleMembers(), TeamFixtures.overweightRobot(),
                TeamFixtures.completeDocuments());

        assertEquals(List.of(RobotClassRule.CODE), codesOf(wrongClass));
        assertEquals(List.of(RobotSpecificationRule.CODE), codesOf(overweight));
    }

    @Test
    void collectsEveryViolationInsteadOfStoppingAtTheFirstOne() {
        EligibilityVerdict verdict = evaluate(TeamFixtures.membersWithUnderageCompetitor(),
                TeamFixtures.overweightRobot(), TeamFixtures.incompleteDocuments());

        assertEquals(List.of(AgeRangeRule.CODE, RobotSpecificationRule.CODE, RequiredDocumentsRule.CODE),
                codesOf(verdict));
        assertEquals(3, verdict.reasons().size());
    }

    private EligibilityVerdict evaluate(List<Member> members, Robot robot, List<TeamDocument> documents) {
        TeamRegistration registration = new TeamRegistration(TeamId.of("TEAM-1"), CompetitionId.of("COMP-1"),
                junior.id(), "Delta Bots", members, robot, documents);
        return policy.verdictFor(new EligibilityRequest(registration, junior, REFERENCE_DATE));
    }

    private List<String> codesOf(EligibilityVerdict verdict) {
        return verdict.violations().stream().map(EligibilityViolation::ruleCode).distinct().toList();
    }
}
