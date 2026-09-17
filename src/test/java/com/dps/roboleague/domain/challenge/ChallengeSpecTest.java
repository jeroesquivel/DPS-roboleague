package com.dps.roboleague.domain.challenge;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.scoring.ContributionKind;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.PenaltyCode;
import com.dps.roboleague.domain.scoring.PenaltyDefinition;
import com.dps.roboleague.domain.scoring.ScoreBreakdown;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.rule.ObjectiveScoringRule;
import com.dps.roboleague.domain.scoring.rule.ThresholdBonusRule;
import com.dps.roboleague.domain.scoring.rule.TimeScoringRule;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChallengeSpecTest {

    private static final MetricKey TIME = MetricKey.of("TIME");
    private static final MetricKey OBJECTIVES = MetricKey.of("OBJECTIVES");
    private static final MetricKey DESIGN = MetricKey.of("DESIGN");
    private static final PenaltyCode RESTART = PenaltyCode.of("RESTART");

    private final ChallengeSpec challenge = new ChallengeSpec(ChallengeId.of("RESCUE"), "Rescue mission",
            List.of(MetricDefinition.required(TIME, MetricKind.TIME_SECONDS, "s"),
                    MetricDefinition.required(OBJECTIVES, MetricKind.OBJECTIVE_COUNT, "objectives"),
                    MetricDefinition.optional(DESIGN, MetricKind.JUDGE_CRITERION, "points")),
            List.of(new ObjectiveScoringRule(OBJECTIVES, Points.of(10), 5)),
            List.of(new PenaltyDefinition(RESTART, "manual restart", Points.of(3))), 2);

    @Test
    void acceptsAMeasurementSetThatCoversEveryRequiredMetric() {
        assertDoesNotThrow(() -> challenge.validate(complete()));
    }

    @Test
    void rejectsAMeasurementSetWithoutARequiredMetric() {
        MeasurementSet incomplete = MeasurementSet.empty().with(TIME, MetricValue.of("95.5"));

        DomainException error = assertThrows(DomainException.class, () -> challenge.validate(incomplete));

        assertTrue(error.getMessage().contains("OBJECTIVES"));
    }

    @Test
    void rejectsMetricsThatTheChallengeDoesNotDefine() {
        MeasurementSet unexpected = complete().with(MetricKey.of("BATTERY"), MetricValue.of("10"));

        DomainException error = assertThrows(DomainException.class, () -> challenge.validate(unexpected));

        assertTrue(error.getMessage().contains("BATTERY"));
    }

    @Test
    void rejectsValuesThatDoNotMatchTheKindOfTheMetric() {
        MeasurementSet fractionalObjectives = complete().with(OBJECTIVES, MetricValue.of("3.5"));

        assertThrows(DomainException.class, () -> challenge.validate(fractionalObjectives));
    }

    @Test
    void rejectsAttemptsBeyondTheConfiguredLimit() {
        assertDoesNotThrow(() -> challenge.requireAttemptWithinLimit(2));
        assertThrows(DomainException.class, () -> challenge.requireAttemptWithinLimit(3));
    }

    @Test
    void acceptsIncidentsThatTheRulebookDefines() {
        assertDoesNotThrow(() -> challenge.validateIncidents(List.of(IncidentReport.once(RESTART))));
    }

    @Test
    void rejectsIncidentsThatTheRulebookDoesNotDefineWhenTheResultIsCaptured() {
        DomainException error = assertThrows(DomainException.class,
                () -> challenge.validateIncidents(List.of(IncidentReport.once(PenaltyCode.of("SABOTAGE")))));

        assertTrue(error.getMessage().contains("SABOTAGE"));
    }

    @Test
    void requiresAtLeastOneScoringRule() {
        DomainException error = assertThrows(DomainException.class,
                () -> new ChallengeSpec(ChallengeId.of("EMPTY"), "Empty challenge",
                        List.of(MetricDefinition.required(TIME, MetricKind.TIME_SECONDS, "s")), List.of(), List.of(),
                        2));

        assertTrue(error.getMessage().contains("scoring rule"));
    }

    @Test
    void scoreCombinesEveryScoringRuleWithTheChallengesOwnPenaltyCatalog() {
        ChallengeSpec multiRuleChallenge = new ChallengeSpec(ChallengeId.of("RESCUE"), "Rescue mission",
                List.of(MetricDefinition.required(TIME, MetricKind.TIME_SECONDS, "s"),
                        MetricDefinition.required(OBJECTIVES, MetricKind.OBJECTIVE_COUNT, "objectives")),
                List.of(new TimeScoringRule(TIME, Duration.ofSeconds(120), Points.of("0.50"), Points.of(30)),
                        new ObjectiveScoringRule(OBJECTIVES, Points.of(10), 5)),
                List.of(new PenaltyDefinition(RESTART, "manual restart", Points.of(3))), 2);
        ScoringContext context = new ScoringContext(
                MeasurementSet.empty().with(TIME, MetricValue.of("95.5")).with(OBJECTIVES, MetricValue.of(4)),
                List.of(), List.of(IncidentReport.once(RESTART)));

        ScoreBreakdown breakdown = multiRuleChallenge.score(context);

        assertEquals(3, breakdown.contributions().size());
        assertEquals(Points.of("-3.00"), breakdown.totalOf(ContributionKind.PENALTY));
        assertEquals(Points.of("49.25"), breakdown.total());
    }

    @Test
    void scoreSeparatesWhatWasEarnedFromBonusesAndPenalties() {
        ChallengeSpec multiRuleChallenge = new ChallengeSpec(ChallengeId.of("RESCUE"), "Rescue mission",
                List.of(MetricDefinition.required(OBJECTIVES, MetricKind.OBJECTIVE_COUNT, "objectives")),
                List.of(new ObjectiveScoringRule(OBJECTIVES, Points.of(10), 5),
                        new ThresholdBonusRule(OBJECTIVES, ThresholdBonusRule.Comparison.AT_LEAST,
                                new BigDecimal("5"), Points.of(15))),
                List.of(new PenaltyDefinition(RESTART, "manual restart", Points.of(3))), 2);
        ScoringContext context = new ScoringContext(MeasurementSet.empty().with(OBJECTIVES, MetricValue.of(5)),
                List.of(), List.of(IncidentReport.once(RESTART)));

        ScoreBreakdown breakdown = multiRuleChallenge.score(context);

        assertEquals(Points.of("50.00"), breakdown.totalOf(ContributionKind.EARNED));
        assertEquals(Points.of("15.00"), breakdown.totalOf(ContributionKind.BONUS));
        assertEquals(Points.of("-3.00"), breakdown.totalOf(ContributionKind.PENALTY));
        assertEquals(Points.of("62.00"), breakdown.total());
    }

    private MeasurementSet complete() {
        return MeasurementSet.empty()
                .with(TIME, MetricValue.of("95.5"))
                .with(OBJECTIVES, MetricValue.of(4));
    }
}
