package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.port.in.GenerateStandings;
import com.dps.roboleague.application.port.in.PublishStandings;
import com.dps.roboleague.application.port.in.RecalculateStandings;
import com.dps.roboleague.application.port.in.ResolveAppeal;
import com.dps.roboleague.application.port.in.SubmitAppeal;
import com.dps.roboleague.support.RescueEditionFixture;
import com.dps.roboleague.domain.appeal.AppealStatus;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.ranking.StandingEntry;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.result.ResultCorrection;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.result.RunStatus;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.PenaltyCode;
import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.DomainException;
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
                List.of(IncidentReport.once(RescueEditionFixture.RESTART)));
        edition.module().generateStandingsUseCase()
                .execute(new GenerateStandings.Command(edition.competitionId(), edition.categoryId(),
                        TestEdition.ACTOR));
        edition.module().publishStandingsUseCase()
                .execute(new PublishStandings.Command(edition.competitionId(), edition.categoryId(),
                        TestEdition.ACTOR));
    }

    @Test
    void anAcceptedAppealCorrectsTheRunWithoutLosingTheOriginalValues() {
        resolve(submitAppeal(), true, Optional.of(new ResolveAppeal.Correction(
                edition.measurements("95.5", 5, "42"), List.of())));

        RunResult run = edition.runResult(deltaRun);
        ResultCorrection correction = run.corrections().getFirst();

        assertEquals(RunStatus.CORRECTED, run.status());
        assertEquals(MetricValue.of(4), run.originalMeasurements().require(RescueEditionFixture.OBJECTIVES));
        assertEquals(MetricValue.of(5), run.currentMeasurements().require(RescueEditionFixture.OBJECTIVES));
        assertTrue(correction.sourceAppeal().isPresent());
        assertEquals(List.of(AuditAction.RESULT_CAPTURED, AuditAction.RESULT_CORRECTED),
                edition.auditActionsFor(deltaRun.value()));
    }

    @Test
    void theRecalculationReordersTheStandingsWithTheSameRulebookVersion() {
        resolve(submitAppeal(), true, Optional.of(new ResolveAppeal.Correction(
                edition.measurements("95.5", 5, "42"), List.of())));

        Standings recalculated = edition.module().recalculateStandingsUseCase()
                .execute(new RecalculateStandings.Command(edition.competitionId(), edition.categoryId(),
                        "objective granted on appeal", TestEdition.ACTOR));

        assertEquals(2, recalculated.revision());
        assertFalse(recalculated.isFinal());
        assertEquals(RulebookVersion.first(), recalculated.rulebookVersion());
        assertEquals(List.of(delta, omega), recalculated.entries().stream().map(StandingEntry::teamId).toList());
        assertEquals(Points.of("85.75"), recalculated.entryFor(delta).orElseThrow().totalPoints());
        assertEquals(2, edition.standingsHistory().size());
    }

    @Test
    void aRejectedAppealLeavesTheCapturedResultUntouched() {
        AppealStatus status = resolve(submitAppeal(), false, Optional.empty());

        RunResult run = edition.runResult(deltaRun);

        assertEquals(AppealStatus.REJECTED, status);
        assertEquals(RunStatus.CAPTURED, run.status());
        assertTrue(run.corrections().isEmpty());
    }

    @Test
    void anAppealIsNotResolvedWhenTheCorrectionItCarriesIsRejected() {
        AppealId appealId = submitAppeal();
        // OBJECTIVES es OBJECTIVE_COUNT: 4.5 no es un valor que el desafío acepte.
        MeasurementSet invalid = MeasurementSet.empty()
                .with(RescueEditionFixture.TIME, MetricValue.of("95.5"))
                .with(RescueEditionFixture.OBJECTIVES, MetricValue.of("4.5"))
                .with(RescueEditionFixture.ENERGY, MetricValue.of("42"));

        assertThrows(DomainException.class,
                () -> resolve(appealId, true, Optional.of(new ResolveAppeal.Correction(invalid, List.of()))));

        assertEquals(AppealStatus.SUBMITTED, edition.appeal(appealId).status());
        assertEquals(RunStatus.CAPTURED, edition.runResult(deltaRun).status());
        assertTrue(edition.runResult(deltaRun).corrections().isEmpty());
    }

    @Test
    void anAppealIsNotResolvedWhenItReportsAnIncidentTheRulebookDoesNotDefine() {
        AppealId appealId = submitAppeal();
        List<IncidentReport> unknown = List.of(IncidentReport.once(PenaltyCode.of("SABOTAGE")));

        assertThrows(DomainException.class, () -> resolve(appealId, true, Optional.of(
                new ResolveAppeal.Correction(edition.measurements("95.5", 5, "42"), unknown))));

        assertEquals(AppealStatus.SUBMITTED, edition.appeal(appealId).status());
    }

    private AppealId submitAppeal() {
        return edition.module().submitAppealUseCase().execute(new SubmitAppeal.Command(deltaRun, delta,
                "the fourth objective was completed before the buzzer", "delta-captain"));
    }

    private AppealStatus resolve(AppealId appealId, boolean accepted,
            Optional<ResolveAppeal.Correction> correction) {
        return edition.module().resolveAppealUseCase().execute(new ResolveAppeal.Command(appealId, accepted, "head-judge",
                "decision based on the video review", correction, "head-judge"));
    }

    private List<AuditAction> actionsOf(List<AuditEvent> events) {
        return events.stream().map(AuditEvent::action).toList();
    }
}
