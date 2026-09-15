package com.dps.roboleague.support;

import com.dps.roboleague.application.port.in.CaptureRunResult;
import com.dps.roboleague.application.port.in.CreateCompetition;
import com.dps.roboleague.application.port.in.CreateSeason;
import com.dps.roboleague.application.port.in.PublishRulebook;
import com.dps.roboleague.application.port.in.RegisterTeam;
import com.dps.roboleague.application.port.in.ScheduleRound;
import com.dps.roboleague.demo.DemoRulebook;
import com.dps.roboleague.domain.challenge.ChallengeSpec;
import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.JudgeEvaluation;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.schedule.TimeSlot;
import com.dps.roboleague.domain.shared.AgeRange;
import com.dps.roboleague.domain.shared.ArenaId;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DateRange;
import com.dps.roboleague.domain.shared.JudgeId;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.SeasonId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.team.Member;
import com.dps.roboleague.domain.team.Robot;
import com.dps.roboleague.domain.team.TeamDocument;
import com.dps.roboleague.infrastructure.config.RoboLeagueModule;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Builds a ready to use edition (season, competition, rulebook) on top of the in-memory adapters so
 * integration tests only describe the behaviour under test.
 */
public final class TestEdition {

    public static final String ACTOR = "test-actor";
    public static final Instant NOW = Instant.parse("2026-03-02T10:00:00Z");

    private final RoboLeagueModule module;
    private final CompetitionId competitionId;
    private final CategoryId categoryId;

    private TestEdition(RoboLeagueModule module, CompetitionId competitionId, CategoryId categoryId) {
        this.module = module;
        this.competitionId = competitionId;
        this.categoryId = categoryId;
    }

    public static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    public static TestEdition start() {
        return start(RoboLeagueModule.inMemory(fixedClock()));
    }

    public static TestEdition start(RoboLeagueModule module) {
        SeasonId seasonId = module.createSeason().execute(new CreateSeason.Command("Season 2026", 2026,
                DateRange.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), ACTOR));
        CompetitionId competitionId = module.createCompetition().execute(new CreateCompetition.Command(seasonId,
                "National Open", DateRange.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5)),
                List.of(new CreateCompetition.CategoryDraft("Junior", AgeRange.between(12, 17),
                        TeamFixtures.RESCUE_BOT)),
                ACTOR));
        CategoryId categoryId = module.competitions().findById(competitionId).orElseThrow().categories()
                .getFirst().id();
        publishRulebook(module, competitionId, DemoRulebook.rescueChallenge());
        return new TestEdition(module, competitionId, categoryId);
    }

    public RulebookVersion publishRulebookWith(ScoringRule scoringRule) {
        ChallengeSpec challenge = new ChallengeSpec(DemoRulebook.CHALLENGE_ID, "Rescue mission",
                DemoRulebook.rescueChallenge().metrics(), scoringRule, 2);
        return publishRulebook(module, competitionId, challenge);
    }

    private static RulebookVersion publishRulebook(RoboLeagueModule module, CompetitionId competitionId,
            ChallengeSpec challenge) {
        return module.publishRulebook().execute(new PublishRulebook.Command(competitionId, List.of(challenge),
                DemoRulebook.eligibilityPolicy(), DemoRulebook.tiebreaks(), ACTOR));
    }

    public RegisterTeam.Outcome register(String name, List<Member> members, Robot robot,
            List<TeamDocument> documents) {
        return module.registerTeam().execute(new RegisterTeam.Command(competitionId, categoryId, name, members, robot,
                documents, ACTOR));
    }

    public TeamId registerEligibleTeam(String name) {
        return register(name, TeamFixtures.eligibleMembers(), TeamFixtures.eligibleRobot(),
                TeamFixtures.completeDocuments()).teamId();
    }

    public RoundId scheduleRound(int ordinal, List<ScheduleRound.HeatDraft> heats) {
        return module.scheduleRound().execute(new ScheduleRound.Command(competitionId, categoryId,
                DemoRulebook.CHALLENGE_ID, ordinal, heats, ACTOR));
    }

    public RoundId scheduleRoundFor(int ordinal, List<TeamId> teams) {
        List<ScheduleRound.HeatDraft> heats = IntStream.range(0, teams.size())
                .mapToObj(index -> heat(teams.get(index), "A1",
                        LocalDateTime.of(2026, 3, 2, 10, 0).plusMinutes(20L * index)))
                .toList();
        return scheduleRound(ordinal, heats);
    }

    public ScheduleRound.HeatDraft heat(TeamId teamId, String arena, LocalDateTime start) {
        return new ScheduleRound.HeatDraft(teamId, ArenaId.of(arena), new TimeSlot(start, Duration.ofMinutes(15)),
                Set.of(JudgeId.of("J1"), JudgeId.of("J2")));
    }

    public RunId capture(RoundId roundId, TeamId teamId, String seconds, int objectives, String energy,
            List<Integer> judgeScores, List<IncidentReport> incidents) {
        return module.captureRunResult().execute(new CaptureRunResult.Command(roundId, teamId, 1,
                measurements(seconds, objectives, energy), evaluations(judgeScores), incidents, ACTOR));
    }

    public List<JudgeEvaluation> evaluations(List<Integer> judgeScores) {
        return IntStream.range(0, judgeScores.size())
                .mapToObj(index -> new JudgeEvaluation(JudgeId.of("J" + (index + 1)), DemoRulebook.DESIGN,
                        Points.of(judgeScores.get(index).longValue())))
                .toList();
    }

    public MeasurementSet measurements(String seconds, int objectives, String energy) {
        return MeasurementSet.empty()
                .with(DemoRulebook.TIME, MetricValue.of(seconds))
                .with(DemoRulebook.OBJECTIVES, MetricValue.of(objectives))
                .with(DemoRulebook.ENERGY, MetricValue.of(energy));
    }

    public RoboLeagueModule module() {
        return module;
    }

    public CompetitionId competitionId() {
        return competitionId;
    }

    public CategoryId categoryId() {
        return categoryId;
    }
}
