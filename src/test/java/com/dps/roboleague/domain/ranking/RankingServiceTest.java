package com.dps.roboleague.domain.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricKey;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.ranking.rule.FastestMetricTiebreak;
import com.dps.roboleague.domain.ranking.rule.FewestPenaltiesTiebreak;
import com.dps.roboleague.domain.ranking.rule.HighestSingleRunTiebreak;
import com.dps.roboleague.domain.scoring.ScoreBreakdown;
import com.dps.roboleague.domain.scoring.ScoringRuleCode;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RankingServiceTest {

    private static final MetricKey TIME = MetricKey.of("TIME");
    private static final List<TiebreakRule> TIEBREAKS = List.of(new HighestSingleRunTiebreak(),
            new FewestPenaltiesTiebreak(), new FastestMetricTiebreak(TIME));

    private final RankingService rankingService = new RankingService();

    @Test
    void ordersTeamsByTotalPointsFromHighestToLowest() {
        TeamScoreSummary alpha = summary("ALPHA", run("R1", "40", null, "90"));
        TeamScoreSummary beta = summary("BETA", run("R2", "70", null, "95"));

        List<StandingEntry> standings = rankingService.rank(List.of(alpha, beta), TIEBREAKS);

        assertEquals(List.of("BETA", "ALPHA"), teamsOf(standings));
        assertEquals(List.of(1, 2), positionsOf(standings));
    }

    @Test
    void breaksATieWithTheFirstRuleThatDiscriminates() {
        TeamScoreSummary steady = summary("STEADY", run("R1", "30", null, "90"), run("R2", "30", null, "91"));
        TeamScoreSummary explosive = summary("EXPLOSIVE", run("R3", "50", null, "92"), run("R4", "10", null, "93"));

        List<StandingEntry> standings = rankingService.rank(List.of(steady, explosive), TIEBREAKS);

        assertEquals(List.of("EXPLOSIVE", "STEADY"), teamsOf(standings));
        assertEquals(List.of(HighestSingleRunTiebreak.CODE), codesOf(standings.get(1)));
        assertTrue(standings.get(1).appliedTiebreaks().getFirst().description().contains("highest single run"));
    }

    @Test
    void appliesTheNextTiebreakWhenTheFirstOneIsAlsoTied() {
        TeamScoreSummary clean = summary("CLEAN", run("R1", "63", "-3", "90"));
        TeamScoreSummary punished = summary("PUNISHED", run("R2", "70", "-10", "90"));

        List<StandingEntry> standings = rankingService.rank(List.of(clean, punished), TIEBREAKS);

        assertEquals(List.of("CLEAN", "PUNISHED"), teamsOf(standings));
        assertEquals(List.of(FewestPenaltiesTiebreak.CODE), codesOf(standings.get(1)));
    }

    @Test
    void sharesThePositionWhenNoTiebreakCanSeparateTheTeams() {
        TeamScoreSummary first = summary("AAA", run("R1", "60", null, "90"));
        TeamScoreSummary second = summary("BBB", run("R2", "60", null, "90"));
        TeamScoreSummary third = summary("CCC", run("R3", "50", null, "95"));

        List<StandingEntry> standings = rankingService.rank(List.of(first, second, third), TIEBREAKS);

        assertEquals(List.of(1, 1, 3), positionsOf(standings));
        assertTrue(standings.get(1).appliedTiebreaks().isEmpty());
    }

    @Test
    void usesTheFastestRecordedTimeWhenPointsAndPenaltiesRemainTied() {
        TeamScoreSummary steady = summary("AAA", run("R1", "30", null, "85"), run("R2", "30", null, "90"));
        TeamScoreSummary fastest = summary("ZZZ", run("R3", "30", null, "95"), run("R4", "30", null, "80"));

        List<StandingEntry> standings = rankingService.rank(List.of(steady, fastest), TIEBREAKS);

        assertEquals(List.of("ZZZ", "AAA"), teamsOf(standings));
        assertEquals(List.of(1, 2), positionsOf(standings));
        assertEquals(List.of(FastestMetricTiebreak.CODE), codesOf(standings.get(1)));
    }

    @Test
    void aTeamWithATimeMeasurementRanksAheadOfOneWithoutIt() {
        TeamScoreSummary missingTime = summary("AAA", run("R1", "60", null, null));
        TeamScoreSummary measured = summary("ZZZ", run("R2", "60", null, "90"));

        List<StandingEntry> standings = rankingService.rank(List.of(missingTime, measured), TIEBREAKS);

        assertEquals(List.of("ZZZ", "AAA"), teamsOf(standings));
        assertEquals(List.of(1, 2), positionsOf(standings));
        assertEquals(List.of(FastestMetricTiebreak.CODE), codesOf(standings.get(1)));
    }

    @Test
    void teamsWithoutTimeMeasurementsStillShareTheirPositionWhenOtherwiseTied() {
        TeamScoreSummary second = summary("BBB", run("R2", "60", null, null));
        TeamScoreSummary first = summary("AAA", run("R1", "60", null, null));

        List<StandingEntry> standings = rankingService.rank(List.of(second, first), TIEBREAKS);

        assertEquals(List.of("AAA", "BBB"), teamsOf(standings));
        assertEquals(List.of(1, 1), positionsOf(standings));
        assertTrue(standings.stream().allMatch(entry -> entry.appliedTiebreaks().isEmpty()));
    }

    private TeamScoreSummary summary(String teamId, ScoredRun... runs) {
        return new TeamScoreSummary(TeamId.of(teamId), List.of(runs));
    }

    private ScoredRun run(String runId, String basePoints, String penaltyPoints, String seconds) {
        List<ScoreContribution> contributions = new ArrayList<>();
        contributions.add(ScoreContribution.earned(ScoringRuleCode.of("CHALLENGE"), "base score",
                Points.of(basePoints)));
        if (penaltyPoints != null) {
            contributions.add(ScoreContribution.penalty(ScoringRuleCode.of("CHALLENGE"), "penalty",
                    Points.of(penaltyPoints)));
        }
        MeasurementSet measurements = seconds == null
                ? MeasurementSet.empty()
                : MeasurementSet.empty().with(TIME, MetricValue.of(seconds));
        return new ScoredRun(RunId.of(runId), ChallengeId.of("RESCUE"), measurements,
                new ScoreBreakdown(contributions));
    }

    private List<String> codesOf(StandingEntry entry) {
        return entry.appliedTiebreaks().stream().map(AppliedTiebreak::code).toList();
    }

    private List<String> teamsOf(List<StandingEntry> standings) {
        return standings.stream().map(entry -> entry.teamId().value()).toList();
    }

    private List<Integer> positionsOf(List<StandingEntry> standings) {
        return standings.stream().map(StandingEntry::position).toList();
    }
}
