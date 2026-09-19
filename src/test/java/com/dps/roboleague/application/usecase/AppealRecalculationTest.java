package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.port.in.GenerateStandings;
import com.dps.roboleague.application.port.in.PublishRulebook;
import com.dps.roboleague.application.port.in.PublishStandings;
import com.dps.roboleague.application.port.in.RecalculateStandings;
import com.dps.roboleague.application.port.in.ResolveAppeal;
import com.dps.roboleague.application.port.in.SubmitAppeal;
import com.dps.roboleague.application.port.out.AppealRepository;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.support.RescueEditionFixture;
import com.dps.roboleague.domain.appeal.Appeal;
import com.dps.roboleague.domain.appeal.AppealStatus;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.ranking.StandingEntry;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.ranking.rule.FastestMetricTiebreak;
import com.dps.roboleague.domain.ranking.rule.FewestPenaltiesTiebreak;
import com.dps.roboleague.domain.result.ResultCorrection;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.result.RunStatus;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.PenaltyCode;
import com.dps.roboleague.domain.scoring.rule.ObjectiveScoringRule;
import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.infrastructure.id.SequentialIdGenerator;
import com.dps.roboleague.infrastructure.memory.InMemoryRunResultRepository;
import com.dps.roboleague.support.TestEdition;
import java.util.ArrayList;
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
    void aNewRulebookDoesNotReplaceHistoricalScoringOrStandingsTiebreaksDuringRecalculation() {
        resolve(submitAppeal(), true, Optional.of(new ResolveAppeal.Correction(
                edition.measurements("124", 5, "52"), List.of())));
        RulebookVersion newVersion = edition.module().publishRulebookUseCase()
                .execute(new PublishRulebook.Command(edition.competitionId(),
                        List.of(RescueEditionFixture.challengeScoredBy(
                                List.of(new ObjectiveScoringRule(RescueEditionFixture.OBJECTIVES, Points.of(100), 5)))),
                        RescueEditionFixture.eligibilityPolicy(),
                        List.of(new FastestMetricTiebreak(RescueEditionFixture.TIME)), TestEdition.ACTOR));

        Standings recalculated = edition.module().recalculateStandingsUseCase()
                .execute(new RecalculateStandings.Command(edition.competitionId(), edition.categoryId(),
                        "corrected measurements reviewed after a new rulebook was published", TestEdition.ACTOR));

        assertEquals(RulebookVersion.of(2), newVersion);
        assertEquals(RulebookVersion.first(), recalculated.rulebookVersion());
        assertEquals(2, recalculated.revision());
        assertFalse(recalculated.isFinal());
        assertEquals(Points.of("71.50"), recalculated.entryFor(delta).orElseThrow().totalPoints());
        assertEquals(Points.of("71.50"), recalculated.entryFor(omega).orElseThrow().totalPoints());
        assertEquals(List.of(delta, omega), recalculated.entries().stream().map(StandingEntry::teamId).toList());
        assertEquals(FewestPenaltiesTiebreak.CODE,
                recalculated.entryFor(omega).orElseThrow().appliedTiebreaks().getFirst().code());
        Standings original = edition.standingsHistory().getFirst();
        assertTrue(original.isFinal());
        assertEquals(Points.of("60.75"), original.entryFor(delta).orElseThrow().totalPoints());
    }

    @Test
    void anAppealFromAnotherTeamIsRejectedWithoutSavingOrAuditingIt() {
        InMemoryRunResultRepository runs = new InMemoryRunResultRepository();
        runs.save(edition.runResult(deltaRun));
        List<Appeal> savedAppeals = new ArrayList<>();
        List<AuditEvent> recordedEvents = new ArrayList<>();
        AppealRepository appeals = new AppealRepository() {
            @Override
            public void save(Appeal appeal) {
                savedAppeals.add(appeal);
            }

            @Override
            public Optional<Appeal> findById(AppealId id) {
                return savedAppeals.stream().filter(appeal -> appeal.id().equals(id)).findFirst();
            }
        };
        AuditLog auditLog = new AuditLog() {
            @Override
            public void record(AuditEvent event) {
                recordedEvents.add(event);
            }

            @Override
            public List<AuditEvent> findBySubject(String subject) {
                return recordedEvents.stream().filter(event -> event.subject().equals(subject)).toList();
            }
        };
        SubmitAppeal submit = new SubmitAppealUseCase(runs, appeals, new SequentialIdGenerator(), auditLog,
                TestEdition.fixedClock());

        assertThrows(DomainException.class,
                () -> submit.execute(new SubmitAppeal.Command(deltaRun, omega, "claim on another team's run",
                        "omega-captain")));

        assertTrue(savedAppeals.isEmpty());
        assertTrue(recordedEvents.isEmpty());
        assertEquals(RunStatus.CAPTURED, edition.runResult(deltaRun).status());
    }

    @Test
    void acceptingAnAppealWithoutACorrectionRecordsTheDecisionWithoutChangingResultsOrStandings() {
        AppealId appealId = submitAppeal();
        Standings published = edition.latestStandings();

        AppealStatus status = resolve(appealId, true, Optional.empty());

        assertEquals(AppealStatus.ACCEPTED, status);
        assertTrue(edition.appeal(appealId).decision().isPresent());
        RunResult run = edition.runResult(deltaRun);
        assertEquals(RunStatus.CAPTURED, run.status());
        assertEquals(run.originalMeasurements(), run.currentMeasurements());
        assertTrue(run.corrections().isEmpty());
        assertEquals(published, edition.latestStandings());
        assertEquals(1, edition.standingsHistory().size());
        assertEquals(List.of(AuditAction.APPEAL_SUBMITTED, AuditAction.APPEAL_RESOLVED),
                edition.auditActionsFor(appealId.value()));
        assertEquals(List.of(AuditAction.RESULT_CAPTURED), edition.auditActionsFor(deltaRun.value()));
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
