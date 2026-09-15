package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.port.in.GenerateStandings;
import com.dps.roboleague.application.port.in.PublishStandings;
import com.dps.roboleague.application.port.in.RecalculateStandings;
import com.dps.roboleague.application.port.in.ResolveAppeal;
import com.dps.roboleague.application.port.in.SubmitAppeal;
import com.dps.roboleague.demo.DemoRulebook;
import com.dps.roboleague.domain.appeal.AppealStatus;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.ranking.StandingEntry;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.result.ResultCorrection;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.result.RunStatus;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.support.TestEdition;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppealRecalculationTest {

    private final TestEdition edition = TestEdition.start();
    private final TeamId delta = edition.registerEligibleTeam("Delta Bots");
    private final TeamId omega = edition.registerEligibleTeam("Omega Crew");

    private RunId deltaRun;

    @BeforeEach
    void publishTheFirstStandings() {
        RoundId roundId = edition.scheduleRoundFor(1, List.of(delta, omega));
        deltaRun = edition.capture(roundId, delta, "95.5", 4, "42", List.of(8, 9), List.of());
        edition.capture(roundId, omega, "105", 5, "55", List.of(7, 7),
                List.of(IncidentReport.once(DemoRulebook.RESTART)));
        edition.module().generateStandings()
                .execute(new GenerateStandings.Command(edition.competitionId(), edition.categoryId(),
                        TestEdition.ACTOR));
        edition.module().publishStandings()
                .execute(new PublishStandings.Command(edition.competitionId(), edition.categoryId(),
                        TestEdition.ACTOR));
    }

    @Test
    void anAcceptedAppealCorrectsTheRunWithoutLosingTheOriginalValues() {
        resolve(submitAppeal(), true, Optional.of(new ResolveAppeal.Correction(
                edition.measurements("95.5", 5, "42"), List.of())));

        RunResult run = edition.module().runResults().findById(deltaRun).orElseThrow();
        ResultCorrection correction = run.corrections().getFirst();

        assertEquals(RunStatus.CORRECTED, run.status());
        assertEquals(MetricValue.of(4), run.originalMeasurements().require(DemoRulebook.OBJECTIVES));
        assertEquals(MetricValue.of(5), run.currentMeasurements().require(DemoRulebook.OBJECTIVES));
        assertTrue(correction.sourceAppeal().isPresent());
        assertEquals(List.of(AuditAction.RESULT_CAPTURED, AuditAction.RESULT_CORRECTED),
                actionsOf(edition.module().auditLog().findBySubject(deltaRun.value())));
    }

    @Test
    void theRecalculationReordersTheStandingsWithTheSameRulebookVersion() {
        resolve(submitAppeal(), true, Optional.of(new ResolveAppeal.Correction(
                edition.measurements("95.5", 5, "42"), List.of())));

        Standings recalculated = edition.module().recalculateStandings()
                .execute(new RecalculateStandings.Command(edition.competitionId(), edition.categoryId(),
                        "objective granted on appeal", TestEdition.ACTOR));

        assertEquals(2, recalculated.revision());
        assertFalse(recalculated.isFinal());
        assertEquals(RulebookVersion.first(), recalculated.rulebookVersion());
        assertEquals(List.of(delta, omega), recalculated.entries().stream().map(StandingEntry::teamId).toList());
        assertEquals(Points.of("85.75"), recalculated.entryFor(delta).orElseThrow().totalPoints());
        assertEquals(2, edition.module().standings()
                .findHistory(edition.competitionId(), edition.categoryId()).size());
    }

    @Test
    void aRejectedAppealLeavesTheCapturedResultUntouched() {
        AppealStatus status = resolve(submitAppeal(), false, Optional.empty());

        RunResult run = edition.module().runResults().findById(deltaRun).orElseThrow();

        assertEquals(AppealStatus.REJECTED, status);
        assertEquals(RunStatus.CAPTURED, run.status());
        assertTrue(run.corrections().isEmpty());
    }

    private AppealId submitAppeal() {
        return edition.module().submitAppeal().execute(new SubmitAppeal.Command(deltaRun, delta,
                "the fourth objective was completed before the buzzer", "delta-captain"));
    }

    private AppealStatus resolve(AppealId appealId, boolean accepted,
            Optional<ResolveAppeal.Correction> correction) {
        return edition.module().resolveAppeal().execute(new ResolveAppeal.Command(appealId, accepted, "head-judge",
                "decision based on the video review", correction, "head-judge"));
    }

    private List<AuditAction> actionsOf(List<AuditEvent> events) {
        return events.stream().map(AuditEvent::action).toList();
    }
}
