package com.dps.roboleague.domain.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.scoring.rule.CompositeScoringRule;
import com.dps.roboleague.domain.scoring.rule.JudgePanelScoringRule;
import com.dps.roboleague.domain.scoring.rule.ObjectiveScoringRule;
import com.dps.roboleague.domain.scoring.rule.PenaltyScoringRule;
import com.dps.roboleague.domain.scoring.rule.PrecisionScoringRule;
import com.dps.roboleague.domain.scoring.rule.ResourceScoringRule;
import com.dps.roboleague.domain.scoring.rule.ThresholdBonusRule;
import com.dps.roboleague.domain.scoring.rule.TimeScoringRule;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.JudgeId;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScoringRulesTest {

    private static final MetricKey TIME = MetricKey.of("TIME");
    private static final MetricKey OBJECTIVES = MetricKey.of("OBJECTIVES");
    private static final MetricKey PRECISION = MetricKey.of("PRECISION");
    private static final MetricKey ENERGY = MetricKey.of("ENERGY");
    private static final MetricKey DESIGN = MetricKey.of("DESIGN");
    private static final PenaltyCode RESTART = PenaltyCode.of("RESTART");

    @Test
    void timeRuleRewardsEverySecondSavedAgainstTheReference() {
        TimeScoringRule rule = new TimeScoringRule(TIME, Duration.ofSeconds(120), Points.of("0.50"), Points.of(30));

        assertEquals(Points.of("12.25"), rule.breakdownFor(measured(TIME, "95.5")).total());
    }

    @Test
    void timeRuleGivesNoPointsWhenTheReferenceIsExceeded() {
        TimeScoringRule rule = new TimeScoringRule(TIME, Duration.ofSeconds(120), Points.of("0.50"), Points.of(30));

        assertEquals(Points.ZERO, rule.breakdownFor(measured(TIME, "130")).total());
    }

    @Test
    void timeRuleNeverExceedsItsMaximum() {
        TimeScoringRule rule = new TimeScoringRule(TIME, Duration.ofSeconds(120), Points.of("0.50"), Points.of(10));

        assertEquals(Points.of(10), rule.breakdownFor(measured(TIME, "10")).total());
    }

    @Test
    void objectiveRuleIgnoresObjectivesReportedAboveTheMaximum() {
        ObjectiveScoringRule rule = new ObjectiveScoringRule(OBJECTIVES, Points.of(10), 5);

        assertEquals(Points.of(50), rule.breakdownFor(measured(OBJECTIVES, "7")).total());
    }

    @Test
    void precisionRuleScalesTheMaximumByTheAchievedRatio() {
        PrecisionScoringRule rule = new PrecisionScoringRule(PRECISION, Points.of(20));

        assertEquals(Points.of("15.00"), rule.breakdownFor(measured(PRECISION, "0.75")).total());
    }

    @Test
    void resourceRuleOnlyDeductsTheConsumptionAboveTheAllowance() {
        ResourceScoringRule rule = new ResourceScoringRule(ENERGY, new BigDecimal("50"), Points.of(1));

        assertEquals(Points.ZERO, rule.breakdownFor(measured(ENERGY, "42")).total());
        assertEquals(Points.of(-5), rule.breakdownFor(measured(ENERGY, "55")).total());
    }

    @Test
    void judgePanelRuleAveragesTheEvaluationsOfItsCriterion() {
        JudgePanelScoringRule rule = new JudgePanelScoringRule(DESIGN, new BigDecimal("2"));
        ScoringContext context = new ScoringContext(MeasurementSet.empty(),
                List.of(evaluation("J1", 8), evaluation("J2", 9), evaluation("J3", 7)), List.of());

        assertEquals(Points.of(16), rule.breakdownFor(context).total());
    }

    @Test
    void judgePanelRuleExplainsThatNoEvaluationWasRecorded() {
        JudgePanelScoringRule rule = new JudgePanelScoringRule(DESIGN, BigDecimal.ONE);
        ScoreBreakdown breakdown = rule.breakdownFor(ScoringContext.of(MeasurementSet.empty()));

        assertEquals(Points.ZERO, breakdown.total());
        assertTrue(breakdown.contributions().getFirst().explanation().contains("no evaluations"));
    }

    @Test
    void penaltyRuleDeductsOnceForEachOccurrence() {
        PenaltyScoringRule rule = new PenaltyScoringRule(
                List.of(new PenaltyDefinition(RESTART, "manual restart", Points.of(3))));
        ScoringContext context = new ScoringContext(MeasurementSet.empty(), List.of(),
                List.of(new IncidentReport(RESTART, 2)));

        assertEquals(Points.of(-6), rule.breakdownFor(context).total());
    }

    @Test
    void penaltyRuleRejectsIncidentsThatTheRulebookDoesNotDefine() {
        PenaltyScoringRule rule = new PenaltyScoringRule(List.of());
        ScoringContext context = new ScoringContext(MeasurementSet.empty(), List.of(),
                List.of(IncidentReport.once(RESTART)));

        assertThrows(DomainException.class, () -> rule.apply(context));
    }

    @Test
    void bonusRuleIsGrantedOnlyWhenTheThresholdIsReached() {
        ThresholdBonusRule rule = new ThresholdBonusRule(OBJECTIVES, ThresholdBonusRule.Comparison.AT_LEAST,
                new BigDecimal("5"), Points.of(15));

        assertEquals(Points.of(15), rule.breakdownFor(measured(OBJECTIVES, "5")).total());
        assertEquals(Points.ZERO, rule.breakdownFor(measured(OBJECTIVES, "4")).total());
    }

    @Test
    void compositeRuleKeepsOneContributionPerRuleAndAddsThemUp() {
        CompositeScoringRule rule = CompositeScoringRule.of("CHALLENGE",
                new TimeScoringRule(TIME, Duration.ofSeconds(120), Points.of("0.50"), Points.of(30)),
                new ObjectiveScoringRule(OBJECTIVES, Points.of(10), 5),
                new PenaltyScoringRule(List.of(new PenaltyDefinition(RESTART, "manual restart", Points.of(3)))));
        ScoringContext context = new ScoringContext(
                MeasurementSet.empty().with(TIME, MetricValue.of("95.5")).with(OBJECTIVES, MetricValue.of(4)),
                List.of(), List.of(IncidentReport.once(RESTART)));

        ScoreBreakdown breakdown = rule.breakdownFor(context);

        assertEquals(3, breakdown.contributions().size());
        assertEquals(Points.of("-3.00"), breakdown.totalFor(PenaltyScoringRule.CODE));
        assertEquals(Points.of("49.25"), breakdown.total());
    }

    private ScoringContext measured(MetricKey key, String amount) {
        return ScoringContext.of(MeasurementSet.empty().with(key, MetricValue.of(amount)));
    }

    private JudgeEvaluation evaluation(String judge, int score) {
        return new JudgeEvaluation(JudgeId.of(judge), DESIGN, Points.of(score));
    }
}
