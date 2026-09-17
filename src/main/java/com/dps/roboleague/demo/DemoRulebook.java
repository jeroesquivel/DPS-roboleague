package com.dps.roboleague.demo;

import com.dps.roboleague.domain.challenge.ChallengeSpec;
import com.dps.roboleague.domain.challenge.MetricDefinition;
import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.challenge.MetricKind;
import com.dps.roboleague.domain.eligibility.EligibilityPolicy;
import com.dps.roboleague.domain.eligibility.rule.AgeRangeRule;
import com.dps.roboleague.domain.eligibility.rule.RequiredDocumentsRule;
import com.dps.roboleague.domain.eligibility.rule.RobotClassRule;
import com.dps.roboleague.domain.eligibility.rule.RobotSpecificationRule;
import com.dps.roboleague.domain.eligibility.rule.TeamCompositionRule;
import com.dps.roboleague.domain.ranking.TiebreakRule;
import com.dps.roboleague.domain.ranking.rule.FastestMetricTiebreak;
import com.dps.roboleague.domain.ranking.rule.FewestPenaltiesTiebreak;
import com.dps.roboleague.domain.ranking.rule.HighestSingleRunTiebreak;
import com.dps.roboleague.domain.scoring.PenaltyCode;
import com.dps.roboleague.domain.scoring.PenaltyDefinition;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.scoring.rule.JudgePanelScoringRule;
import com.dps.roboleague.domain.scoring.rule.ObjectiveScoringRule;
import com.dps.roboleague.domain.scoring.rule.ResourceScoringRule;
import com.dps.roboleague.domain.scoring.rule.ThresholdBonusRule;
import com.dps.roboleague.domain.scoring.rule.TimeScoringRule;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.team.Dimensions;
import com.dps.roboleague.domain.team.DocumentType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/** El reglamento que recorre el ejecutable de ejemplo. Los tests arman el suyo. */
final class DemoRulebook {

    static final ChallengeId CHALLENGE_ID = ChallengeId.of("RESCUE");
    static final MetricKey TIME = MetricKey.of("TIME");
    static final MetricKey OBJECTIVES = MetricKey.of("OBJECTIVES");
    static final MetricKey ENERGY = MetricKey.of("ENERGY");
    static final MetricKey DESIGN = MetricKey.of("DESIGN");
    static final PenaltyCode RESTART = PenaltyCode.of("RESTART");
    static final PenaltyCode OUT_OF_BOUNDS = PenaltyCode.of("OUT_OF_BOUNDS");

    private DemoRulebook() {
    }

    static ChallengeSpec rescueChallenge() {
        List<MetricDefinition> metrics = List.of(
                MetricDefinition.required(TIME, MetricKind.TIME_SECONDS, "s"),
                MetricDefinition.required(OBJECTIVES, MetricKind.OBJECTIVE_COUNT, "objectives"),
                MetricDefinition.required(ENERGY, MetricKind.RESOURCE_UNITS, "mAh"),
                MetricDefinition.optional(DESIGN, MetricKind.JUDGE_CRITERION, "points"));
        return new ChallengeSpec(CHALLENGE_ID, "Rescue mission", metrics, scoringRules(), penalties(), 2);
    }

    static List<ScoringRule> scoringRules() {
        return List.of(
                new TimeScoringRule(TIME, Duration.ofSeconds(120), Points.of("0.50"), Points.of(30)),
                new ObjectiveScoringRule(OBJECTIVES, Points.of(10), 5),
                new JudgePanelScoringRule(DESIGN, BigDecimal.ONE),
                new ResourceScoringRule(ENERGY, new BigDecimal("50"), Points.of(1)),
                new ThresholdBonusRule(OBJECTIVES, ThresholdBonusRule.Comparison.AT_LEAST, new BigDecimal("5"),
                        Points.of(15)));
    }

    static List<PenaltyDefinition> penalties() {
        return List.of(
                new PenaltyDefinition(RESTART, "manual restart", Points.of(3)),
                new PenaltyDefinition(OUT_OF_BOUNDS, "robot out of bounds", Points.of(5)));
    }

    static EligibilityPolicy eligibilityPolicy() {
        return EligibilityPolicy.of(
                new AgeRangeRule(),
                new TeamCompositionRule(2, 4, 18),
                new RobotClassRule(),
                new RobotSpecificationRule(new BigDecimal("3.000"), new Dimensions(200, 200, 200)),
                new RequiredDocumentsRule(Set.of(DocumentType.PARENTAL_CONSENT, DocumentType.TECHNICAL_SHEET)));
    }

    static List<TiebreakRule> tiebreaks() {
        return List.of(new HighestSingleRunTiebreak(), new FewestPenaltiesTiebreak(), new FastestMetricTiebreak(TIME));
    }
}
