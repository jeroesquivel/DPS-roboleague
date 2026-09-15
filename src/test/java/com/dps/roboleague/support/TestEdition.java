package com.dps.roboleague.support;

import com.dps.roboleague.application.port.in.CaptureRunResult;
import com.dps.roboleague.application.port.in.CreateCompetition;
import com.dps.roboleague.application.port.in.CreateSeason;
import com.dps.roboleague.application.port.in.FindAppeal;
import com.dps.roboleague.application.port.in.FindAuditTrail;
import com.dps.roboleague.application.port.in.FindRound;
import com.dps.roboleague.application.port.in.FindRunResult;
import com.dps.roboleague.application.port.in.FindTeamRegistration;
import com.dps.roboleague.application.port.in.GetStandings;
import com.dps.roboleague.application.port.in.PublishRulebook;
import com.dps.roboleague.application.port.in.RegisterTeam;
import com.dps.roboleague.application.port.in.ScheduleRound;
import com.dps.roboleague.domain.appeal.Appeal;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.challenge.ChallengeSpec;
import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.challenge.MetricValue;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.schedule.Round;
import com.dps.roboleague.domain.schedule.TimeSlot;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.JudgeEvaluation;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.shared.AgeRange;
import com.dps.roboleague.domain.shared.AppealId;
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
import com.dps.roboleague.domain.team.Member;
import com.dps.roboleague.domain.team.Robot;
import com.dps.roboleague.domain.team.TeamDocument;
import com.dps.roboleague.domain.team.TeamRegistration;
import com.dps.roboleague.infrastructure.config.RoboLeagueCompositionRoot;
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
 * Arma una edición lista para usar (temporada, competencia, reglamento) sobre los adaptadores en
 * memoria, de modo que los tests de integración sólo describan el comportamiento bajo prueba.
 *
 * <p>Todo lo que lee pasa por un caso de uso de consulta: ningún test alcanza un repositorio.
 */
public final class TestEdition {

    public static final String ACTOR = "test-actor";
    public static final Instant NOW = Instant.parse("2026-03-02T10:00:00Z");
    public static final LocalDate FIRST_DAY = LocalDate.of(2026, 3, 1);
    public static final LocalDate LAST_DAY = LocalDate.of(2026, 3, 5);

    private final RoboLeagueCompositionRoot module;
    private final CompetitionId competitionId;
    private final CategoryId categoryId;

    private TestEdition(RoboLeagueCompositionRoot module, CompetitionId competitionId, CategoryId categoryId) {
        this.module = module;
        this.competitionId = competitionId;
        this.categoryId = categoryId;
    }

    public static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    public static TestEdition start() {
        return start(RoboLeagueCompositionRoot.inMemory(fixedClock()));
    }

    public static TestEdition start(RoboLeagueCompositionRoot module) {
        SeasonId seasonId = module.createSeasonUseCase().execute(new CreateSeason.Command("Season 2026", 2026,
                DateRange.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), ACTOR));
        CreateCompetition.Result competition = module.createCompetitionUseCase()
                .execute(new CreateCompetition.Command(seasonId, "National Open",
                        DateRange.of(FIRST_DAY, LAST_DAY),
                        List.of(new CreateCompetition.CategoryDraft("Junior", AgeRange.between(12, 17),
                                TeamFixtures.RESCUE_BOT)),
                        ACTOR));
        publishRulebook(module, competition.competitionId(), RescueEditionFixture.rescueChallenge());
        return new TestEdition(module, competition.competitionId(), competition.firstCategory());
    }

    public RulebookVersion publishRulebookWith(ScoringRule scoringRule) {
        return publishRulebook(module, competitionId, RescueEditionFixture.challengeScoredBy(scoringRule));
    }

    private static RulebookVersion publishRulebook(RoboLeagueCompositionRoot module, CompetitionId competitionId,
            ChallengeSpec challenge) {
        return module.publishRulebookUseCase().execute(new PublishRulebook.Command(competitionId, List.of(challenge),
                RescueEditionFixture.eligibilityPolicy(), RescueEditionFixture.tiebreaks(), ACTOR));
    }

    // --- escritura ---

    public RegisterTeam.Outcome register(String name, List<Member> members, Robot robot,
            List<TeamDocument> documents) {
        return module.registerTeamUseCase().execute(new RegisterTeam.Command(competitionId, categoryId, name, members,
                robot, documents, ACTOR));
    }

    public TeamId registerEligibleTeam(String name) {
        return register(name, TeamFixtures.eligibleMembers(), TeamFixtures.eligibleRobot(),
                TeamFixtures.completeDocuments()).teamId();
    }

    public RoundId scheduleRound(int ordinal, List<ScheduleRound.HeatDraft> heats) {
        return module.scheduleRoundUseCase().execute(new ScheduleRound.Command(competitionId, categoryId,
                RescueEditionFixture.CHALLENGE_ID, ordinal, heats, ACTOR));
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
        return module.captureRunResultUseCase().execute(new CaptureRunResult.Command(roundId, teamId, 1,
                measurements(seconds, objectives, energy), evaluations(judgeScores), incidents, ACTOR));
    }

    public List<JudgeEvaluation> evaluations(List<Integer> judgeScores) {
        return IntStream.range(0, judgeScores.size())
                .mapToObj(index -> new JudgeEvaluation(JudgeId.of("J" + (index + 1)), RescueEditionFixture.DESIGN,
                        Points.of(judgeScores.get(index).longValue())))
                .toList();
    }

    public MeasurementSet measurements(String seconds, int objectives, String energy) {
        return MeasurementSet.empty()
                .with(RescueEditionFixture.TIME, MetricValue.of(seconds))
                .with(RescueEditionFixture.OBJECTIVES, MetricValue.of(objectives))
                .with(RescueEditionFixture.ENERGY, MetricValue.of(energy));
    }

    // --- consulta, siempre a través de un puerto de entrada ---

    public RunResult runResult(RunId runId) {
        return module.findRunResultUseCase().execute(new FindRunResult.Command(runId));
    }

    public TeamRegistration registration(TeamId teamId) {
        return module.findTeamRegistrationUseCase().execute(new FindTeamRegistration.Command(teamId));
    }

    public Round round(RoundId roundId) {
        return module.findRoundUseCase().execute(new FindRound.Command(roundId));
    }

    public Appeal appeal(AppealId appealId) {
        return module.findAppealUseCase().execute(new FindAppeal.Command(appealId));
    }

    public Standings latestStandings() {
        return module.getStandingsUseCase().execute(new GetStandings.Command(competitionId, categoryId)).latest();
    }

    public List<Standings> standingsHistory() {
        return module.getStandingsUseCase().execute(new GetStandings.Command(competitionId, categoryId)).history();
    }

    public List<AuditAction> auditActionsFor(String subject) {
        return FindAuditTrail.actionsOf(
                module.findAuditTrailUseCase().execute(new FindAuditTrail.Command(subject)));
    }

    public RoboLeagueCompositionRoot module() {
        return module;
    }

    public CompetitionId competitionId() {
        return competitionId;
    }

    public CategoryId categoryId() {
        return categoryId;
    }
}
