package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.port.in.RegisterTeam;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.eligibility.rule.AgeRangeRule;
import com.dps.roboleague.domain.team.RegistrationStatus;
import com.dps.roboleague.domain.team.TeamRegistration;
import com.dps.roboleague.support.TeamFixtures;
import com.dps.roboleague.support.TestEdition;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegisterTeamUseCaseTest {

    private final TestEdition edition = TestEdition.start();

    @Test
    void acceptsATeamThatSatisfiesTheEligibilityPolicyOfTheActiveRulebook() {
        RegisterTeam.Outcome outcome = edition.register("Delta Bots", TeamFixtures.eligibleMembers(),
                TeamFixtures.eligibleRobot(), TeamFixtures.completeDocuments());

        assertTrue(outcome.isAccepted());
        assertTrue(outcome.verdict().isEligible());
        assertEquals(List.of(AuditAction.TEAM_REGISTERED),
                actionsOf(edition.module().auditLog().findBySubject(outcome.teamId().value())));
    }

    @Test
    void storesTheRejectionReasonsOfATeamThatBreaksTheRules() {
        RegisterTeam.Outcome outcome = edition.register("Rookies", TeamFixtures.membersWithUnderageCompetitor(),
                TeamFixtures.eligibleRobot(), TeamFixtures.incompleteDocuments());

        TeamRegistration stored = edition.module().registrations().findById(outcome.teamId()).orElseThrow();

        assertFalse(outcome.isAccepted());
        assertEquals(RegistrationStatus.REJECTED, stored.status());
        assertEquals(2, stored.rejectionReasons().size());
        assertTrue(stored.rejectionReasons().getFirst().startsWith(AgeRangeRule.CODE));
        assertEquals(List.of(AuditAction.TEAM_REJECTED),
                actionsOf(edition.module().auditLog().findBySubject(outcome.teamId().value())));
    }

    private List<AuditAction> actionsOf(List<AuditEvent> events) {
        return events.stream().map(AuditEvent::action).toList();
    }
}
