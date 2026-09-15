package com.dps.roboleague.domain.appeal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AppealTest {

    private static final Instant SUBMITTED_AT = Instant.parse("2026-03-02T11:00:00Z");

    private final Appeal appeal = new Appeal(AppealId.of("APPEAL-1"), RunId.of("RUN-1"), TeamId.of("TEAM-1"),
            "the fourth objective was completed", SUBMITTED_AT);

    @Test
    void isSubmittedUntilItIsResolved() {
        assertEquals(AppealStatus.SUBMITTED, appeal.status());
        assertTrue(appeal.decision().isEmpty());
    }

    @Test
    void keepsTheDecisionThatAcceptedIt() {
        appeal.accept(decision("the video review confirms the objective"));

        assertTrue(appeal.isAccepted());
        assertEquals("the video review confirms the objective", appeal.decision().orElseThrow().rationale());
    }

    @Test
    void cannotBeResolvedTwice() {
        appeal.reject(decision("the claim is not supported by the recordings"));

        assertThrows(DomainException.class, () -> appeal.accept(decision("second thoughts")));
        assertEquals(AppealStatus.REJECTED, appeal.status());
    }

    @Test
    void rejectsDecisionsThatPredateTheSubmission() {
        AppealDecision early = new AppealDecision("head-judge", "too early", SUBMITTED_AT.minusSeconds(60));

        assertThrows(DomainException.class, () -> appeal.accept(early));
    }

    private AppealDecision decision(String rationale) {
        return new AppealDecision("head-judge", rationale, SUBMITTED_AT.plusSeconds(1800));
    }
}
