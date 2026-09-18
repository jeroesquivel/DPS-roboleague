package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.port.in.CaptureRunResult;
import com.dps.roboleague.application.port.in.CalculateRunScore;
import com.dps.roboleague.support.RescueEditionFixture;
import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.result.RunStatus;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.scoring.ContributionKind;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.PenaltyCode;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.support.TestEdition;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaptureRunResultUseCaseTest {

    private final TestEdition edition = TestEdition.start();
    private final TeamId delta = edition.registerEligibleTeam("Delta Bots");
    private final RoundId roundId = edition.scheduleRoundFor(1, List.of(delta));

    @Test
    void capturesTheRunPinningTheRulebookVersionInForce() {
        RunId runId = edition.capture(roundId, delta, "95.5", 4, "42", List.of(8, 9), List.of());

        RunResult stored = edition.runResult(runId);

        assertEquals(RulebookVersion.first(), stored.rulebookVersion());
        assertEquals(RunStatus.CAPTURED, stored.status());
        assertEquals(MetricValue.of(4), stored.originalMeasurements().require(RescueEditionFixture.OBJECTIVES));
        assertEquals(2, stored.evaluations().size());
    }

    @Test
    void capturesTheLastAllowedAttemptWithoutReplacingTheFirst() {
        RunId first = edition.capture(roundId, delta, "95.5", 4, "42", List.of(8, 9), List.of());

        RunId second = capture(2, edition.measurements("90", 5, "40"));

        assertNotEquals(first, second);
        assertEquals(1, edition.runResult(first).attemptNumber());
        assertEquals(2, edition.runResult(second).attemptNumber());
        assertEquals(MetricValue.of(4), edition.runResult(first).originalMeasurements()
                .require(RescueEditionFixture.OBJECTIVES));
        assertEquals(MetricValue.of(5), edition.runResult(second).originalMeasurements()
                .require(RescueEditionFixture.OBJECTIVES));
        assertEquals(Points.of("60.75"), edition.module().calculateRunScoreUseCase()
                .execute(new CalculateRunScore.Command(first)).total());
        assertEquals(Points.of("80.00"), edition.module().calculateRunScoreUseCase()
                .execute(new CalculateRunScore.Command(second)).total());
    }

    @Test
    void rejectsAnUnknownIncidentAndAllowsTheCorrectedCaptureOfThatAttempt() {
        assertThrows(DomainException.class, () -> edition.capture(roundId, delta, "95.5", 4, "42",
                List.of(8, 9), List.of(IncidentReport.once(PenaltyCode.of("UNKNOWN")))));

        RunId run = edition.capture(roundId, delta, "95.5", 4, "42", List.of(8, 9),
                List.of(IncidentReport.once(RescueEditionFixture.RESTART)));

        CalculateRunScore.RunScore score = edition.module().calculateRunScoreUseCase()
                .execute(new CalculateRunScore.Command(run));
        assertEquals(Points.of("57.75"), score.total());
        assertEquals(Points.of("-3.00"), score.breakdown().totalOf(ContributionKind.PENALTY));
        assertEquals(1, edition.runResult(run).originalIncidents().size());
    }

    @Test
    void rejectsMeasurementsThatTheChallengeDoesNotAccept() {
        MeasurementSet withoutObjectives = MeasurementSet.empty()
                .with(RescueEditionFixture.TIME, MetricValue.of("95.5"))
                .with(RescueEditionFixture.ENERGY, MetricValue.of("42"));

        assertThrows(DomainException.class, () -> capture(1, withoutObjectives));
    }

    @Test
    void rejectsAttemptsBeyondTheLimitOfTheChallenge() {
        assertThrows(DomainException.class, () -> capture(3, edition.measurements("95.5", 4, "42")));
    }

    @Test
    void rejectsCapturingTheSameAttemptTwice() {
        edition.capture(roundId, delta, "95.5", 4, "42", List.of(8, 9), List.of());

        DomainException error = assertThrows(DomainException.class,
                () -> capture(1, edition.measurements("90", 5, "40")));

        assertTrue(error.getMessage().contains("already captured"));
    }

    @Test
    void rejectsTeamsWithoutAHeatInTheRound() {
        TeamId omega = edition.registerEligibleTeam("Omega Crew");

        assertThrows(DomainException.class, () -> edition.module().captureRunResultUseCase()
                .execute(new CaptureRunResult.Command(roundId, omega, 1, edition.measurements("95.5", 4, "42"),
                        List.of(), List.of(), TestEdition.ACTOR)));
    }

    private RunId capture(int attempt, MeasurementSet measurements) {
        return edition.module().captureRunResultUseCase().execute(new CaptureRunResult.Command(roundId, delta, attempt,
                measurements, List.of(), List.of(), TestEdition.ACTOR));
    }
}
