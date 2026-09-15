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
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.rule.PenaltyScoringRule;
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
        assertEquals(List.of(HighestSingleRunTiebreak.CODE), standings.get(1).appliedTiebreaks());
    }

    @Test
    void appliesTheNextTiebreakWhenTheFirstOneIsAlsoTied() {
        TeamScoreSummary clean = summary("CLEAN", run("R1", "63", "-3", "90"));
        TeamScoreSummary punished = summary("PUNISHED", run("R2", "70", "-10", "90"));

        List<StandingEntry> standings = rankingService.rank(List.of(clean, punished), TIEBREAKS);

        assertEquals(List.of("CLEAN", "PUNISHED"), teamsOf(standings));
        assertEquals(List.of(FewestPenaltiesTiebreak.CODE), standings.get(1).appliedTiebreaks());
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

    private TeamScoreSummary summary(String teamId, ScoredRun... runs) {
        return new TeamScoreSummary(TeamId.of(teamId), List.of(runs));
    }

    private ScoredRun run(String runId, String basePoints, String penaltyPoints, String seconds) {
        List<ScoreContribution> contributions = new ArrayList<>();
        contributions.add(new ScoreContribution("CHALLENGE", "base score", Points.of(basePoints)));
        if (penaltyPoints != null) {
            contributions.add(new ScoreContribution(PenaltyScoringRule.CODE, "penalty", Points.of(penaltyPoints)));
        }
        return new ScoredRun(RunId.of(runId), ChallengeId.of("RESCUE"),
                MeasurementSet.empty().with(TIME, MetricValue.of(seconds)), new ScoreBreakdown(contributions));
    }

    private List<String> teamsOf(List<StandingEntry> standings) {
        return standings.stream().map(entry -> entry.teamId().value()).toList();
    }

    private List<Integer> positionsOf(List<StandingEntry> standings) {
        return standings.stream().map(StandingEntry::position).toList();
    }
}
