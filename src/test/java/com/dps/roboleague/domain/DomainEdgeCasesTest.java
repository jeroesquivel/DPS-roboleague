package com.dps.roboleague.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.appeal.Appeal;
import com.dps.roboleague.domain.appeal.AppealDecision;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.challenge.ChallengeSpec;
import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricDefinition;
import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.challenge.MetricKind;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.competition.Category;
import com.dps.roboleague.domain.competition.Competition;
import com.dps.roboleague.domain.competition.RobotClass;
import com.dps.roboleague.domain.competition.Season;
import com.dps.roboleague.domain.eligibility.EligibilityRequest;
import com.dps.roboleague.domain.eligibility.EligibilityViolation;
import com.dps.roboleague.domain.eligibility.rule.RobotSpecificationRule;
import com.dps.roboleague.domain.eligibility.rule.TeamCompositionRule;
import com.dps.roboleague.domain.ranking.AppliedTiebreak;
import com.dps.roboleague.domain.ranking.PublicationStatus;
import com.dps.roboleague.domain.ranking.StandingEntry;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.result.ResultCorrection;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.rulebook.Rulebook;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.schedule.Heat;
import com.dps.roboleague.domain.schedule.Round;
import com.dps.roboleague.domain.schedule.ScheduleConflict;
import com.dps.roboleague.domain.schedule.ScheduleConflictDetector;
import com.dps.roboleague.domain.schedule.ScheduleConflictType;
import com.dps.roboleague.domain.schedule.TimeSlot;
import com.dps.roboleague.domain.scoring.ContributionKind;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.JudgeEvaluation;
import com.dps.roboleague.domain.scoring.PenaltyCode;
import com.dps.roboleague.domain.scoring.PenaltyDefinition;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRuleCode;
import com.dps.roboleague.domain.scoring.rule.ObjectiveScoringRule;
import com.dps.roboleague.domain.scoring.rule.ThresholdBonusRule;
import com.dps.roboleague.domain.shared.AgeRange;
import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.ArenaId;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DateRange;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.HeatId;
import com.dps.roboleague.domain.shared.JudgeId;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.SeasonId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.domain.team.Dimensions;
import com.dps.roboleague.domain.team.DocumentType;
import com.dps.roboleague.domain.team.Member;
import com.dps.roboleague.domain.team.MemberRole;
import com.dps.roboleague.domain.team.Robot;
import com.dps.roboleague.domain.team.TeamDocument;
import com.dps.roboleague.domain.team.TeamRegistration;
import com.dps.roboleague.support.RescueEditionFixture;
import com.dps.roboleague.support.TeamFixtures;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class DomainEdgeCasesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);
    private static final Instant NOW = Instant.parse("2026-03-02T10:00:00Z");
    private static final Category CATEGORY = new Category(CategoryId.of("CAT-1"), "Junior",
            AgeRange.between(12, 17), TeamFixtures.RESCUE_BOT);

    @Test
    void rejectsInvalidCodesAndTextValues() {
        assertThrows(DomainException.class, () -> TeamId.of(null));
        assertThrows(DomainException.class, () -> TeamId.of(" "));
        assertThrows(DomainException.class, () -> PenaltyCode.of(null));
        assertThrows(DomainException.class, () -> PenaltyCode.of(" "));
        assertThrows(DomainException.class, () -> ScoringRuleCode.of(null));
        assertThrows(DomainException.class, () -> ScoringRuleCode.of(" "));
        assertThrows(DomainException.class, () -> MetricKey.of(null));
        assertThrows(DomainException.class, () -> MetricKey.of(" "));
        assertThrows(DomainException.class, () -> RobotClass.of(null));
        assertThrows(DomainException.class, () -> RobotClass.of(" "));
        assertThrows(DomainException.class, () -> new EligibilityViolation(null, "reason"));
        assertThrows(DomainException.class, () -> new EligibilityViolation(" ", "reason"));
        assertThrows(DomainException.class, () -> new EligibilityViolation("RULE", null));
        assertThrows(DomainException.class, () -> new EligibilityViolation("RULE", " "));
        assertThrows(DomainException.class, () -> new AppliedTiebreak(null, "description"));
        assertThrows(DomainException.class, () -> new AppliedTiebreak(" ", "description"));
        assertThrows(DomainException.class, () -> new AppliedTiebreak("RULE", null));
        assertThrows(DomainException.class, () -> new AppliedTiebreak("RULE", " "));
        assertThrows(DomainException.class, () -> new AppealDecision(null, "rationale", NOW));
        assertThrows(DomainException.class, () -> new AppealDecision(" ", "rationale", NOW));
        assertThrows(DomainException.class, () -> new AppealDecision("reviewer", null, NOW));
        assertThrows(DomainException.class, () -> new AppealDecision("reviewer", " ", NOW));
        assertThrows(DomainException.class,
                () -> new AuditEvent(NOW, AuditAction.TEAM_REGISTERED, null, "actor", Map.of()));
        assertThrows(DomainException.class,
                () -> new AuditEvent(NOW, AuditAction.TEAM_REGISTERED, " ", "actor", Map.of()));
        assertThrows(DomainException.class,
                () -> new AuditEvent(NOW, AuditAction.TEAM_REGISTERED, "subject", null, Map.of()));
        assertThrows(DomainException.class,
                () -> new AuditEvent(NOW, AuditAction.TEAM_REGISTERED, "subject", " ", Map.of()));
    }

    @Test
    void rejectsInvalidRangesAndNumericValues() {
        assertThrows(DomainException.class, () -> DateRange.of(TODAY, TODAY.minusDays(1)));
        assertThrows(DomainException.class, () -> AgeRange.between(-1, 17));
        assertThrows(DomainException.class, () -> AgeRange.between(18, 17));
        assertFalse(AgeRange.between(12, 17).includes(18));
        assertEquals(Points.of(5), Points.of(5).cappedAt(Points.of(10)));
        assertThrows(DomainException.class, () -> RulebookVersion.of(0));
        assertFalse(RulebookVersion.first().isNewerThan(RulebookVersion.of(2)));
        assertThrows(DomainException.class, () -> MetricValue.of("-0.1"));
        assertEquals(MetricValue.of("1.250"), MetricValue.of(new BigDecimal("1.25")));
        assertEquals(MetricValue.of("1.250"), MetricValue.ofSeconds(Duration.ofMillis(1250)));
        assertFalse(MetricKind.TIME_SECONDS.accepts(new BigDecimal("-1")));
        assertFalse(MetricKind.PRECISION_RATIO.accepts(new BigDecimal("1.1")));
        assertTrue(MetricKind.PRECISION_RATIO.accepts(BigDecimal.ONE));
        assertFalse(MetricKind.OBJECTIVE_COUNT.accepts(new BigDecimal("1.5")));
        assertTrue(MetricKind.OBJECTIVE_COUNT.accepts(new BigDecimal("2")));
        assertTrue(MetricKind.RESOURCE_UNITS.accepts(BigDecimal.ONE));
    }

    @Test
    void validatesScoringValueObjectsAndAlternativeBonusComparison() {
        PenaltyCode penalty = PenaltyCode.of("RESTART");
        ScoringRuleCode scoringRule = ScoringRuleCode.of("RULE");
        MetricKey objectives = MetricKey.of("OBJECTIVES");

        assertThrows(DomainException.class, () -> new PenaltyDefinition(penalty, null, Points.of(1)));
        assertThrows(DomainException.class, () -> new PenaltyDefinition(penalty, " ", Points.of(1)));
        assertThrows(DomainException.class, () -> new PenaltyDefinition(penalty, "restart", Points.of(-1)));
        assertThrows(DomainException.class,
                () -> new ScoreContribution(scoringRule, ContributionKind.EARNED, null, Points.ZERO));
        assertThrows(DomainException.class,
                () -> new ScoreContribution(scoringRule, ContributionKind.EARNED, " ", Points.ZERO));
        assertThrows(DomainException.class, () -> new IncidentReport(penalty, 0));
        assertThrows(DomainException.class,
                () -> new JudgeEvaluation(JudgeId.of("J1"), objectives, Points.of(-1)));
        assertThrows(DomainException.class, () -> new ObjectiveScoringRule(objectives, Points.of(1), 0));

        ThresholdBonusRule atMost = new ThresholdBonusRule(objectives, ThresholdBonusRule.Comparison.AT_MOST,
                new BigDecimal("5"), Points.of(10));
        assertEquals(Points.of(10), atMost.breakdownFor(measured(objectives, "4")).total());
        assertEquals(Points.ZERO, atMost.breakdownFor(measured(objectives, "6")).total());
    }

    @Test
    void validatesTeamAndMemberState() {
        assertThrows(DomainException.class, () -> new Dimensions(0, 1, 1));
        assertThrows(DomainException.class, () -> new Dimensions(1, 0, 1));
        assertThrows(DomainException.class, () -> new Dimensions(1, 1, 0));
        Dimensions limit = new Dimensions(10, 10, 10);
        assertFalse(new Dimensions(11, 1, 1).fitsWithin(limit));
        assertFalse(new Dimensions(1, 11, 1).fitsWithin(limit));
        assertFalse(new Dimensions(1, 1, 11).fitsWithin(limit));

        assertThrows(DomainException.class,
                () -> new Robot(null, TeamFixtures.RESCUE_BOT, BigDecimal.ONE, limit));
        assertThrows(DomainException.class,
                () -> new Robot(" ", TeamFixtures.RESCUE_BOT, BigDecimal.ONE, limit));
        assertThrows(DomainException.class,
                () -> new Robot("Robot", TeamFixtures.RESCUE_BOT, BigDecimal.ZERO, limit));
        assertThrows(DomainException.class, () -> new Member(null, TODAY.minusYears(14), MemberRole.COMPETITOR));
        assertThrows(DomainException.class, () -> new Member(" ", TODAY.minusYears(14), MemberRole.COMPETITOR));
        Member futureMember = new Member("Future", TODAY.plusDays(1), MemberRole.COMPETITOR);
        assertThrows(DomainException.class, () -> futureMember.ageOn(TODAY));
        assertFalse(new Member("Coach", TODAY.minusYears(30), MemberRole.COACH).isCompetitor());
        assertThrows(DomainException.class, () -> new TeamDocument(DocumentType.TECHNICAL_SHEET, null));
        assertThrows(DomainException.class, () -> new TeamDocument(DocumentType.TECHNICAL_SHEET, " "));

        assertThrows(DomainException.class, () -> registration(null, TeamFixtures.eligibleMembers()));
        assertThrows(DomainException.class, () -> registration(" ", TeamFixtures.eligibleMembers()));
        assertThrows(DomainException.class, () -> registration("Team", null));
        assertThrows(DomainException.class, () -> registration("Team", List.of()));
        TeamRegistration registration = registration("Team", TeamFixtures.eligibleMembers());
        assertEquals(CompetitionId.of("COMP-1"), registration.competitionId());
        assertThrows(DomainException.class, () -> registration.reject(List.of()));
    }

    @Test
    void validatesTeamCompositionAndRobotDimensions() {
        TeamCompositionRule composition = new TeamCompositionRule(2, 4, 18);
        assertThrows(DomainException.class, () -> new TeamCompositionRule(0, 4, 18));
        assertThrows(DomainException.class, () -> new TeamCompositionRule(3, 2, 18));

        List<Member> adults = IntStream.range(0, 5)
                .mapToObj(index -> new Member("Adult " + index, TODAY.minusYears(25), MemberRole.COMPETITOR))
                .toList();
        assertEquals(1, composition.evaluate(request(registration("Large", adults))).size());
        assertTrue(composition.evaluate(request(registration("Adults", adults.subList(0, 2)))).isEmpty());
        assertEquals(1, composition.evaluate(request(registration("Coach only",
                List.of(new Member("Coach", TODAY.minusYears(25), MemberRole.COACH))))).size());

        Robot oversized = new Robot("Large", TeamFixtures.RESCUE_BOT, BigDecimal.ONE,
                new Dimensions(201, 200, 200));
        RobotSpecificationRule specification = new RobotSpecificationRule(new BigDecimal("3"),
                new Dimensions(200, 200, 200));
        assertEquals(1, specification.evaluate(request(registration("Large robot",
                TeamFixtures.eligibleMembers(), oversized))).size());
    }

    @Test
    void validatesChallengeAndRulebookBoundaries() {
        MetricKey objectives = MetricKey.of("OBJECTIVES");
        MetricDefinition definition = MetricDefinition.required(objectives, MetricKind.OBJECTIVE_COUNT, "count");
        ObjectiveScoringRule scoringRule = new ObjectiveScoringRule(objectives, Points.of(10), 5);

        assertThrows(DomainException.class,
                () -> new MetricDefinition(objectives, MetricKind.OBJECTIVE_COUNT, null, true));
        assertThrows(DomainException.class,
                () -> new MetricDefinition(objectives, MetricKind.OBJECTIVE_COUNT, " ", true));
        assertThrows(DomainException.class,
                () -> new ChallengeSpec(ChallengeId.of("C"), null, List.of(definition), List.of(scoringRule),
                        List.of(), 1));
        assertThrows(DomainException.class,
                () -> new ChallengeSpec(ChallengeId.of("C"), " ", List.of(definition), List.of(scoringRule),
                        List.of(), 1));
        assertThrows(DomainException.class,
                () -> new ChallengeSpec(ChallengeId.of("C"), "Challenge", List.of(), List.of(scoringRule),
                        List.of(), 1));
        assertThrows(DomainException.class,
                () -> new ChallengeSpec(ChallengeId.of("C"), "Challenge", List.of(definition),
                        List.of(scoringRule), List.of(), 0));

        ChallengeSpec challenge = new ChallengeSpec(ChallengeId.of("C"), "Challenge", List.of(definition),
                List.of(scoringRule), List.of(), 2);
        assertThrows(DomainException.class, () -> challenge.requireAttemptWithinLimit(0));
        Rulebook rulebook = Rulebook.of(CompetitionId.of("COMP-1"), RulebookVersion.first(), TODAY,
                List.of(challenge), RescueEditionFixture.eligibilityPolicy(), RescueEditionFixture.tiebreaks());
        assertThrows(DomainException.class, () -> rulebook.challenge(ChallengeId.of("UNKNOWN")));
    }

    @Test
    void validatesCompetitionAndRankingState() {
        assertThrows(DomainException.class, () -> new Category(CategoryId.of("CAT"), null,
                AgeRange.between(12, 17), TeamFixtures.RESCUE_BOT));
        assertThrows(DomainException.class, () -> new Category(CategoryId.of("CAT"), " ",
                AgeRange.between(12, 17), TeamFixtures.RESCUE_BOT));
        DateRange year = DateRange.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThrows(DomainException.class, () -> new Season(SeasonId.of("S"), null, 2026, year));
        assertThrows(DomainException.class, () -> new Season(SeasonId.of("S"), " ", 2026, year));
        assertThrows(DomainException.class, () -> new Season(SeasonId.of("S"), "Season", 2025, year));

        assertThrows(DomainException.class, () -> competition(null));
        assertThrows(DomainException.class, () -> competition(" "));
        Competition competition = competition("Competition");
        assertThrows(DomainException.class, () -> competition.addCategory(CATEGORY));
        assertThrows(DomainException.class, () -> competition.category(CategoryId.of("UNKNOWN")));
        assertThrows(DomainException.class, competition::requireActiveRulebookVersion);
        competition.activateRulebook(RulebookVersion.of(2));
        assertThrows(DomainException.class, () -> competition.activateRulebook(RulebookVersion.first()));
        assertEquals(SeasonId.of("SEASON-1"), competition.seasonId());

        assertThrows(DomainException.class, () -> new StandingEntry(0, TeamId.of("T"), Points.ZERO, List.of()));
        assertThrows(DomainException.class, () -> new Standings(CompetitionId.of("COMP-1"), CategoryId.of("CAT-1"),
                RulebookVersion.first(), 0, PublicationStatus.PROVISIONAL, NOW, List.of()));
    }

    @Test
    void validatesScheduleEntitiesAndConflictIdentity() {
        assertThrows(DomainException.class,
                () -> new ScheduleConflict(ScheduleConflictType.ARENA_BUSY, null));
        assertThrows(DomainException.class,
                () -> new ScheduleConflict(ScheduleConflictType.ARENA_BUSY, " "));
        assertThrows(DomainException.class, () -> new TimeSlot(LocalDateTime.of(2026, 3, 2, 10, 0), Duration.ZERO));
        assertThrows(DomainException.class,
                () -> new TimeSlot(LocalDateTime.of(2026, 3, 2, 10, 0), Duration.ofMinutes(-1)));
        TimeSlot early = new TimeSlot(LocalDateTime.of(2026, 3, 2, 9, 0), Duration.ofHours(1));
        TimeSlot later = new TimeSlot(LocalDateTime.of(2026, 3, 2, 10, 0), Duration.ofHours(1));
        assertFalse(early.overlaps(later));
        assertFalse(later.overlaps(early));

        Round round = round(1);
        assertThrows(DomainException.class, () -> round(0));
        Heat wrongRound = heat("H1", RoundId.of("OTHER"), TeamId.of("TEAM-1"), Set.of(JudgeId.of("J1")));
        assertThrows(DomainException.class, () -> round.schedule(wrongRound));
        Heat first = heat("H1", round.id(), TeamId.of("TEAM-1"), Set.of(JudgeId.of("J1")));
        round.schedule(first);
        Heat duplicateTeam = heat("H2", round.id(), TeamId.of("TEAM-1"), Set.of(JudgeId.of("J2")));
        assertThrows(DomainException.class, () -> round.schedule(duplicateTeam));
        assertEquals(1, round.ordinal());
        assertThrows(DomainException.class,
                () -> heat("EMPTY", round.id(), TeamId.of("TEAM-2"), Set.of()));
        assertTrue(new ScheduleConflictDetector().detect(List.of(first), first).isEmpty());
    }

    @Test
    void validatesAppealCorrectionAndRunState() {
        Appeal appeal = new Appeal(AppealId.of("A1"), RunId.of("RUN-1"), TeamId.of("TEAM-1"), "claim", NOW);
        assertThrows(DomainException.class,
                () -> new Appeal(AppealId.of("A2"), RunId.of("RUN-1"), TeamId.of("TEAM-1"), null, NOW));
        assertThrows(DomainException.class,
                () -> new Appeal(AppealId.of("A2"), RunId.of("RUN-1"), TeamId.of("TEAM-1"), " ", NOW));
        assertFalse(appeal.isAccepted());
        assertEquals(RunId.of("RUN-1"), appeal.runId());
        assertEquals(TeamId.of("TEAM-1"), appeal.teamId());
        assertEquals("claim", appeal.claim());
        assertEquals(NOW, appeal.submittedAt());

        MeasurementSet measurements = MeasurementSet.empty();
        assertThrows(DomainException.class,
                () -> new ResultCorrection(NOW, null, "reason", measurements, List.of(), Optional.empty()));
        assertThrows(DomainException.class,
                () -> new ResultCorrection(NOW, " ", "reason", measurements, List.of(), Optional.empty()));
        assertThrows(DomainException.class,
                () -> new ResultCorrection(NOW, "actor", null, measurements, List.of(), Optional.empty()));
        assertThrows(DomainException.class,
                () -> new ResultCorrection(NOW, "actor", " ", measurements, List.of(), Optional.empty()));
        assertThrows(DomainException.class, () -> run(0));
        RunResult run = run(1);
        assertEquals(HeatId.of("HEAT-1"), run.heatId());
        assertEquals(NOW, run.capturedAt());
        assertThrows(DomainException.class, () -> measurements.require(MetricKey.of("MISSING")));
    }

    private ScoringContext measured(MetricKey key, String value) {
        return ScoringContext.of(MeasurementSet.empty().with(key, MetricValue.of(value)));
    }

    private TeamRegistration registration(String name, List<Member> members) {
        return registration(name, members, TeamFixtures.eligibleRobot());
    }

    private TeamRegistration registration(String name, List<Member> members, Robot robot) {
        return new TeamRegistration(TeamId.of("TEAM-1"), CompetitionId.of("COMP-1"), CATEGORY.id(), name, members,
                robot, TeamFixtures.completeDocuments());
    }

    private EligibilityRequest request(TeamRegistration registration) {
        return new EligibilityRequest(registration, CATEGORY, TODAY);
    }

    private Competition competition(String name) {
        return new Competition(CompetitionId.of("COMP-1"), SeasonId.of("SEASON-1"), name,
                DateRange.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5)), List.of(CATEGORY));
    }

    private Round round(int ordinal) {
        return new Round(RoundId.of("ROUND-1"), CompetitionId.of("COMP-1"), CategoryId.of("CAT-1"),
                ChallengeId.of("CHALLENGE-1"), ordinal, RulebookVersion.first());
    }

    private Heat heat(String id, RoundId roundId, TeamId teamId, Set<JudgeId> judges) {
        return new Heat(HeatId.of(id), roundId, teamId, ArenaId.of("A1"),
                new TimeSlot(LocalDateTime.of(2026, 3, 2, 10, 0), Duration.ofMinutes(15)), judges);
    }

    private RunResult run(int attempt) {
        return new RunResult(RunId.of("RUN-1"), RoundId.of("ROUND-1"), HeatId.of("HEAT-1"), TeamId.of("TEAM-1"),
                ChallengeId.of("CHALLENGE-1"), RulebookVersion.first(), attempt, NOW, MeasurementSet.empty(),
                List.of(), List.of());
    }
}
