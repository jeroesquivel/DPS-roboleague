package com.dps.roboleague.domain.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.PenaltyCode;
import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.HeatId;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RunResultTest {

    private static final MetricKey OBJECTIVES = MetricKey.of("OBJECTIVES");
    private static final Instant CAPTURED_AT = Instant.parse("2026-03-02T10:15:00Z");

    private final RunResult run = capturedRun();

    @Test
    void startsAsCapturedWithoutCorrections() {
        assertEquals(RunStatus.CAPTURED, run.status());
        assertEquals(MetricValue.of(4), run.currentMeasurements().require(OBJECTIVES));
    }

    @Test
    void keepsTheOriginalValuesWhenACorrectionIsApplied() {
        run.applyCorrection(correction(5, CAPTURED_AT.plusSeconds(3600)));

        assertEquals(MetricValue.of(4), run.originalMeasurements().require(OBJECTIVES));
        assertEquals(MetricValue.of(5), run.currentMeasurements().require(OBJECTIVES));
        assertEquals(RunStatus.CORRECTED, run.status());
    }

    @Test
    void scoresWithTheLastCorrectionAndKeepsTheWholeHistory() {
        run.applyCorrection(correction(5, CAPTURED_AT.plusSeconds(3600)));
        run.applyCorrection(correction(3, CAPTURED_AT.plusSeconds(7200)));

        assertEquals(2, run.corrections().size());
        assertEquals(MetricValue.of(3), run.scoringContext().measurements().require(OBJECTIVES));
    }

    @Test
    void rejectsCorrectionsThatPredateTheCapture() {
        ResultCorrection early = correction(5, CAPTURED_AT.minusSeconds(60));

        assertThrows(DomainException.class, () -> run.applyCorrection(early));
    }

    private RunResult capturedRun() {
        return new RunResult(RunId.of("RUN-1"), RoundId.of("ROUND-1"), HeatId.of("HEAT-1"), TeamId.of("TEAM-1"),
                ChallengeId.of("RESCUE"), RulebookVersion.first(), 1, CAPTURED_AT, measurements(4), List.of(),
                List.of(IncidentReport.once(PenaltyCode.of("RESTART"))));
    }

    private ResultCorrection correction(int objectives, Instant appliedAt) {
        return new ResultCorrection(appliedAt, "head-judge", "video review", measurements(objectives), List.of(),
                Optional.of(AppealId.of("APPEAL-1")));
    }

    private MeasurementSet measurements(int objectives) {
        return MeasurementSet.empty().with(OBJECTIVES, MetricValue.of(objectives));
    }
}
