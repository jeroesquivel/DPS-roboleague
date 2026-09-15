package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.port.in.CaptureRunResult;
import com.dps.roboleague.demo.DemoRulebook;
import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.result.RunStatus;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.DomainException;
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

        RunResult stored = edition.module().runResults().findById(runId).orElseThrow();

        assertEquals(RulebookVersion.first(), stored.rulebookVersion());
        assertEquals(RunStatus.CAPTURED, stored.status());
        assertEquals(MetricValue.of(4), stored.originalMeasurements().require(DemoRulebook.OBJECTIVES));
        assertEquals(2, stored.evaluations().size());
    }

    @Test
    void rejectsMeasurementsThatTheChallengeDoesNotAccept() {
        MeasurementSet withoutObjectives = MeasurementSet.empty()
                .with(DemoRulebook.TIME, MetricValue.of("95.5"))
                .with(DemoRulebook.ENERGY, MetricValue.of("42"));

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

        assertThrows(DomainException.class, () -> edition.module().captureRunResult()
                .execute(new CaptureRunResult.Command(roundId, omega, 1, edition.measurements("95.5", 4, "42"),
                        List.of(), List.of(), TestEdition.ACTOR)));
    }

    private RunId capture(int attempt, MeasurementSet measurements) {
        return edition.module().captureRunResult().execute(new CaptureRunResult.Command(roundId, delta, attempt,
                measurements, List.of(), List.of(), TestEdition.ACTOR));
    }
}
