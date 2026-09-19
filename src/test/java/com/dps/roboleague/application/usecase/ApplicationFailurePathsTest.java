package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dps.roboleague.Main;
import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.CalculateRunScore;
import com.dps.roboleague.application.port.in.CaptureRunResult;
import com.dps.roboleague.application.port.in.GenerateStandings;
import com.dps.roboleague.application.port.in.GetStandings;
import com.dps.roboleague.application.port.in.RecalculateStandings;
import com.dps.roboleague.application.port.in.RegisterTeam;
import com.dps.roboleague.application.port.in.ResolveAppeal;
import com.dps.roboleague.application.port.in.ScheduleRound;
import com.dps.roboleague.application.port.in.SubmitAppeal;
import com.dps.roboleague.application.service.CategoryScoringService;
import com.dps.roboleague.domain.appeal.Appeal;
import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.competition.Category;
import com.dps.roboleague.domain.competition.Competition;
import com.dps.roboleague.domain.ranking.RankingService;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.rulebook.Rulebook;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.schedule.Heat;
import com.dps.roboleague.domain.schedule.Round;
import com.dps.roboleague.domain.schedule.ScheduleConflictDetector;
import com.dps.roboleague.domain.schedule.TimeSlot;
import com.dps.roboleague.domain.shared.AgeRange;
import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.ArenaId;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DateRange;
import com.dps.roboleague.domain.shared.HeatId;
import com.dps.roboleague.domain.shared.JudgeId;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.SeasonId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.domain.team.TeamRegistration;
import com.dps.roboleague.infrastructure.config.RoboLeagueCompositionRoot;
import com.dps.roboleague.infrastructure.id.SequentialIdGenerator;
import com.dps.roboleague.infrastructure.memory.InMemoryAppealRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryAuditLog;
import com.dps.roboleague.infrastructure.memory.InMemoryCompetitionRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryRoundRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryRulebookRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryRunResultRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryStandingsRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryTeamRegistrationRepository;
import com.dps.roboleague.support.RescueEditionFixture;
import com.dps.roboleague.support.TeamFixtures;
import com.dps.roboleague.support.TestEdition;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationFailurePathsTest {

    private static final CompetitionId COMPETITION_ID = CompetitionId.of("COMP-1");
    private static final CategoryId CATEGORY_ID = CategoryId.of("CAT-1");
    private static final CategoryId OTHER_CATEGORY_ID = CategoryId.of("CAT-2");
    private static final TeamId TEAM_ID = TeamId.of("TEAM-1");
    private static final RoundId ROUND_ID = RoundId.of("ROUND-1");
    private static final RunId RUN_ID = RunId.of("RUN-1");
    private static final AppealId APPEAL_ID = AppealId.of("APPEAL-1");
    private static final Instant NOW = Instant.parse("2026-03-02T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private InMemoryCompetitionRepository competitions;
    private InMemoryRulebookRepository rulebooks;
    private InMemoryTeamRegistrationRepository registrations;
    private InMemoryRoundRepository rounds;
    private InMemoryRunResultRepository runResults;
    private InMemoryStandingsRepository standings;
    private InMemoryAppealRepository appeals;
    private InMemoryAuditLog auditLog;
    private SequentialIdGenerator ids;
    private CategoryScoringService scoring;

    @BeforeEach
    void setUp() {
        competitions = new InMemoryCompetitionRepository();
        rulebooks = new InMemoryRulebookRepository();
        registrations = new InMemoryTeamRegistrationRepository();
        rounds = new InMemoryRoundRepository();
        runResults = new InMemoryRunResultRepository();
        standings = new InMemoryStandingsRepository();
        appeals = new InMemoryAppealRepository();
        auditLog = new InMemoryAuditLog();
        ids = new SequentialIdGenerator();
        scoring = new CategoryScoringService(rounds, runResults, rulebooks);
    }

    @Test
    void inputPortsReportMissingTopLevelEntities() {
        RoboLeagueCompositionRoot module = RoboLeagueCompositionRoot.inMemory(CLOCK);

        assertThrows(NotFoundException.class,
                () -> module.findCompetitionUseCase().execute(CompetitionId.of("MISSING")));
        assertThrows(NotFoundException.class,
                () -> module.findTeamRegistrationUseCase().execute(TeamId.of("MISSING")));
        assertThrows(NotFoundException.class, () -> module.findRoundUseCase().execute(RoundId.of("MISSING")));
        assertThrows(NotFoundException.class, () -> module.findRunResultUseCase().execute(RunId.of("MISSING")));
        assertThrows(NotFoundException.class, () -> module.findAppealUseCase().execute(AppealId.of("MISSING")));
        assertThrows(NotFoundException.class, () -> module.getStandingsUseCase()
                .execute(new GetStandings.Command(COMPETITION_ID, CATEGORY_ID)));
        assertThrows(NotFoundException.class, () -> module.publishStandingsUseCase()
                .execute(new com.dps.roboleague.application.port.in.PublishStandings.Command(
                        COMPETITION_ID, CATEGORY_ID, "actor")));
        assertThrows(NotFoundException.class, () -> module.recalculateStandingsUseCase()
                .execute(new RecalculateStandings.Command(COMPETITION_ID, CATEGORY_ID, "reason", "actor")));
        assertThrows(NotFoundException.class, () -> module.calculateRunScoreUseCase()
                .execute(new CalculateRunScore.Command(RUN_ID)));
        assertThrows(NotFoundException.class, () -> module.captureRunResultUseCase()
                .execute(new CaptureRunResult.Command(ROUND_ID, TEAM_ID, 1, MeasurementSet.empty(), List.of(),
                        List.of(), "actor")));
        assertThrows(NotFoundException.class, () -> module.submitAppealUseCase()
                .execute(new SubmitAppeal.Command(RUN_ID, TEAM_ID, "claim", "actor")));
        assertThrows(NotFoundException.class, () -> module.resolveAppealUseCase()
                .execute(resolveCommand()));
        assertThrows(NotFoundException.class, () -> module.registerTeamUseCase()
                .execute(registerCommand(COMPETITION_ID)));
        assertThrows(NotFoundException.class, () -> module.generateStandingsUseCase()
                .execute(new GenerateStandings.Command(COMPETITION_ID, CATEGORY_ID, "actor")));
        assertThrows(NotFoundException.class, () -> module.scheduleRoundUseCase()
                .execute(scheduleCommand(TEAM_ID, CATEGORY_ID)));
    }

    @Test
    void useCasesReportMissingRulebooks() {
        Competition competition = activeCompetition();
        competitions.save(competition);

        RegisterTeamUseCase register = new RegisterTeamUseCase(competitions, rulebooks, registrations, ids, auditLog,
                CLOCK);
        GenerateStandingsUseCase generate = new GenerateStandingsUseCase(competitions, rulebooks, standings, scoring,
                new RankingService(), auditLog, CLOCK);
        ScheduleRoundUseCase schedule = new ScheduleRoundUseCase(competitions, rulebooks, registrations, rounds,
                new ScheduleConflictDetector(), ids, auditLog, CLOCK);

        assertThrows(NotFoundException.class, () -> register.execute(registerCommand(COMPETITION_ID)));
        assertThrows(NotFoundException.class,
                () -> generate.execute(new GenerateStandings.Command(COMPETITION_ID, CATEGORY_ID, "actor")));
        assertThrows(NotFoundException.class, () -> schedule.execute(scheduleCommand(TEAM_ID, CATEGORY_ID)));

        Standings current = Standings.provisional(COMPETITION_ID, CATEGORY_ID, RulebookVersion.first(), NOW,
                List.of());
        standings.save(current);
        RecalculateStandingsUseCase recalculate = new RecalculateStandingsUseCase(standings, rulebooks, scoring,
                new RankingService(), auditLog, CLOCK);
        assertThrows(NotFoundException.class,
                () -> recalculate.execute(new RecalculateStandings.Command(COMPETITION_ID, CATEGORY_ID, "reason",
                        "actor")));

        Round round = scheduledRound();
        rounds.save(round);
        CaptureRunResultUseCase capture = new CaptureRunResultUseCase(rounds, rulebooks, runResults, ids, auditLog,
                CLOCK);
        assertThrows(NotFoundException.class,
                () -> capture.execute(new CaptureRunResult.Command(ROUND_ID, TEAM_ID, 1, MeasurementSet.empty(),
                        List.of(), List.of(), "actor")));
        assertThrows(NotFoundException.class, () -> scoring.scoreRun(run(), COMPETITION_ID));
    }

    @Test
    void schedulingReportsMissingAndMismatchedRegistrations() {
        competitions.save(activeCompetition());
        rulebooks.save(rulebook());
        ScheduleRoundUseCase schedule = new ScheduleRoundUseCase(competitions, rulebooks, registrations, rounds,
                new ScheduleConflictDetector(), ids, auditLog, CLOCK);

        assertThrows(NotFoundException.class, () -> schedule.execute(scheduleCommand(TEAM_ID, CATEGORY_ID)));

        TeamRegistration registration = new TeamRegistration(TEAM_ID, COMPETITION_ID, OTHER_CATEGORY_ID, "Team",
                TeamFixtures.eligibleMembers(), TeamFixtures.eligibleRobot(), TeamFixtures.completeDocuments());
        registration.accept();
        registrations.save(registration);
        assertThrows(com.dps.roboleague.domain.shared.DomainException.class,
                () -> schedule.execute(scheduleCommand(TEAM_ID, CATEGORY_ID)));
    }

    @Test
    void scoreCalculationReportsAMissingRound() {
        runResults.save(run());
        CalculateRunScoreUseCase calculate = new CalculateRunScoreUseCase(runResults, rounds, scoring);

        assertThrows(NotFoundException.class,
                () -> calculate.execute(new CalculateRunScore.Command(RUN_ID)));
    }

    @Test
    void appealResolutionReportsEachMissingCorrectionDependency() {
        Appeal appeal = new Appeal(APPEAL_ID, RUN_ID, TEAM_ID, "claim", NOW);
        appeals.save(appeal);
        ResolveAppealUseCase resolve = new ResolveAppealUseCase(appeals, runResults, rounds, rulebooks, auditLog,
                CLOCK);

        assertThrows(NotFoundException.class, () -> resolve.execute(resolveCommand()));
        runResults.save(run());
        assertThrows(NotFoundException.class, () -> resolve.execute(resolveCommand()));
        rounds.save(scheduledRound());
        assertThrows(NotFoundException.class, () -> resolve.execute(resolveCommand()));
    }

    @Test
    void mainEntryPointRunsTheDemo() {
        assertDoesNotThrow(Main::new);
        assertDoesNotThrow(() -> Main.main(new String[0]));
    }

    private Competition activeCompetition() {
        Competition competition = new Competition(COMPETITION_ID, SeasonId.of("SEASON-1"), "Competition",
                DateRange.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5)),
                List.of(category(CATEGORY_ID), category(OTHER_CATEGORY_ID)));
        competition.activateRulebook(RulebookVersion.first());
        return competition;
    }

    private Category category(CategoryId id) {
        return new Category(id, id.value(), AgeRange.between(12, 17), TeamFixtures.RESCUE_BOT);
    }

    private Rulebook rulebook() {
        return Rulebook.of(COMPETITION_ID, RulebookVersion.first(), LocalDate.of(2026, 3, 1),
                List.of(RescueEditionFixture.rescueChallenge()), RescueEditionFixture.eligibilityPolicy(),
                RescueEditionFixture.tiebreaks());
    }

    private Round scheduledRound() {
        Round round = new Round(ROUND_ID, COMPETITION_ID, CATEGORY_ID, RescueEditionFixture.CHALLENGE_ID, 1,
                RulebookVersion.first());
        round.schedule(new Heat(HeatId.of("HEAT-1"), ROUND_ID, TEAM_ID, ArenaId.of("A1"),
                new TimeSlot(LocalDateTime.of(2026, 3, 2, 10, 0), Duration.ofMinutes(15)),
                Set.of(JudgeId.of("J1"))));
        return round;
    }

    private RunResult run() {
        return new RunResult(RUN_ID, ROUND_ID, HeatId.of("HEAT-1"), TEAM_ID,
                RescueEditionFixture.CHALLENGE_ID, RulebookVersion.first(), 1, NOW, MeasurementSet.empty(),
                List.of(), List.of());
    }

    private RegisterTeam.Command registerCommand(CompetitionId competitionId) {
        return new RegisterTeam.Command(competitionId, CATEGORY_ID, "Team", TeamFixtures.eligibleMembers(),
                TeamFixtures.eligibleRobot(), TeamFixtures.completeDocuments(), "actor");
    }

    private ScheduleRound.Command scheduleCommand(TeamId teamId, CategoryId categoryId) {
        ScheduleRound.HeatDraft heat = new ScheduleRound.HeatDraft(teamId, ArenaId.of("A1"),
                new TimeSlot(LocalDateTime.of(2026, 3, 2, 10, 0), Duration.ofMinutes(15)),
                Set.of(JudgeId.of("J1")));
        return new ScheduleRound.Command(COMPETITION_ID, categoryId, RescueEditionFixture.CHALLENGE_ID, 1,
                List.of(heat), "actor");
    }

    private ResolveAppeal.Command resolveCommand() {
        ResolveAppeal.Correction correction = new ResolveAppeal.Correction(MeasurementSet.empty(), List.of());
        return new ResolveAppeal.Command(APPEAL_ID, true, "reviewer", "rationale", Optional.of(correction),
                "actor");
    }
}
