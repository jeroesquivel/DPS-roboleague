package com.dps.roboleague.infrastructure.config;

import com.dps.roboleague.application.port.in.CalculateRunScore;
import com.dps.roboleague.application.port.in.CaptureRunResult;
import com.dps.roboleague.application.port.in.CreateCompetition;
import com.dps.roboleague.application.port.in.CreateSeason;
import com.dps.roboleague.application.port.in.GenerateStandings;
import com.dps.roboleague.application.port.in.PublishRulebook;
import com.dps.roboleague.application.port.in.PublishStandings;
import com.dps.roboleague.application.port.in.RecalculateStandings;
import com.dps.roboleague.application.port.in.RegisterTeam;
import com.dps.roboleague.application.port.in.ResolveAppeal;
import com.dps.roboleague.application.port.in.ScheduleRound;
import com.dps.roboleague.application.port.in.SubmitAppeal;
import com.dps.roboleague.application.port.out.AppealRepository;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.CompetitionRepository;
import com.dps.roboleague.application.port.out.IdGenerator;
import com.dps.roboleague.application.port.out.RoundRepository;
import com.dps.roboleague.application.port.out.RulebookRepository;
import com.dps.roboleague.application.port.out.RunResultRepository;
import com.dps.roboleague.application.port.out.SeasonRepository;
import com.dps.roboleague.application.port.out.StandingsRepository;
import com.dps.roboleague.application.port.out.TeamRegistrationRepository;
import com.dps.roboleague.application.service.CategoryScoreCollector;
import com.dps.roboleague.application.usecase.CalculateRunScoreUseCase;
import com.dps.roboleague.application.usecase.CaptureRunResultUseCase;
import com.dps.roboleague.application.usecase.CreateCompetitionUseCase;
import com.dps.roboleague.application.usecase.CreateSeasonUseCase;
import com.dps.roboleague.application.usecase.GenerateStandingsUseCase;
import com.dps.roboleague.application.usecase.PublishRulebookUseCase;
import com.dps.roboleague.application.usecase.PublishStandingsUseCase;
import com.dps.roboleague.application.usecase.RecalculateStandingsUseCase;
import com.dps.roboleague.application.usecase.RegisterTeamUseCase;
import com.dps.roboleague.application.usecase.ResolveAppealUseCase;
import com.dps.roboleague.application.usecase.ScheduleRoundUseCase;
import com.dps.roboleague.application.usecase.SubmitAppealUseCase;
import com.dps.roboleague.domain.ranking.RankingService;
import com.dps.roboleague.domain.schedule.ScheduleConflictDetector;
import com.dps.roboleague.infrastructure.id.SequentialIdGenerator;
import com.dps.roboleague.infrastructure.memory.InMemoryAppealRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryAuditLog;
import com.dps.roboleague.infrastructure.memory.InMemoryCompetitionRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryRoundRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryRulebookRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryRunResultRepository;
import com.dps.roboleague.infrastructure.memory.InMemorySeasonRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryStandingsRepository;
import com.dps.roboleague.infrastructure.memory.InMemoryTeamRegistrationRepository;
import java.time.Clock;

/**
 * Composition root: the only place where use cases are bound to concrete adapters.
 */
public final class RoboLeagueModule {

    private final SeasonRepository seasons;
    private final CompetitionRepository competitions;
    private final RulebookRepository rulebooks;
    private final TeamRegistrationRepository registrations;
    private final RoundRepository rounds;
    private final RunResultRepository runResults;
    private final StandingsRepository standings;
    private final AppealRepository appeals;
    private final AuditLog auditLog;
    private final CategoryScoreCollector scoreCollector;
    private final RankingService rankingService = new RankingService();
    private final ScheduleConflictDetector conflictDetector = new ScheduleConflictDetector();
    private final IdGenerator idGenerator;
    private final Clock clock;

    public RoboLeagueModule(SeasonRepository seasons, CompetitionRepository competitions, RulebookRepository rulebooks,
            TeamRegistrationRepository registrations, RoundRepository rounds, RunResultRepository runResults,
            StandingsRepository standings, AppealRepository appeals, AuditLog auditLog, IdGenerator idGenerator,
            Clock clock) {
        this.seasons = seasons;
        this.competitions = competitions;
        this.rulebooks = rulebooks;
        this.registrations = registrations;
        this.rounds = rounds;
        this.runResults = runResults;
        this.standings = standings;
        this.appeals = appeals;
        this.auditLog = auditLog;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.scoreCollector = new CategoryScoreCollector(rounds, runResults, rulebooks);
    }

    public static RoboLeagueModule inMemory(Clock clock) {
        return new RoboLeagueModule(new InMemorySeasonRepository(), new InMemoryCompetitionRepository(),
                new InMemoryRulebookRepository(), new InMemoryTeamRegistrationRepository(),
                new InMemoryRoundRepository(), new InMemoryRunResultRepository(), new InMemoryStandingsRepository(),
                new InMemoryAppealRepository(), new InMemoryAuditLog(), new SequentialIdGenerator(), clock);
    }

    public CreateSeason createSeason() {
        return new CreateSeasonUseCase(seasons, idGenerator, auditLog, clock);
    }

    public CreateCompetition createCompetition() {
        return new CreateCompetitionUseCase(seasons, competitions, idGenerator, auditLog, clock);
    }

    public PublishRulebook publishRulebook() {
        return new PublishRulebookUseCase(competitions, rulebooks, auditLog, clock);
    }

    public RegisterTeam registerTeam() {
        return new RegisterTeamUseCase(competitions, rulebooks, registrations, idGenerator, auditLog, clock);
    }

    public ScheduleRound scheduleRound() {
        return new ScheduleRoundUseCase(competitions, rulebooks, registrations, rounds, conflictDetector, idGenerator,
                auditLog, clock);
    }

    public CaptureRunResult captureRunResult() {
        return new CaptureRunResultUseCase(rounds, rulebooks, runResults, idGenerator, auditLog, clock);
    }

    public CalculateRunScore calculateRunScore() {
        return new CalculateRunScoreUseCase(runResults, rounds, scoreCollector);
    }

    public GenerateStandings generateStandings() {
        return new GenerateStandingsUseCase(competitions, rulebooks, standings, scoreCollector, rankingService,
                auditLog, clock);
    }

    public PublishStandings publishStandings() {
        return new PublishStandingsUseCase(standings, auditLog, clock);
    }

    public SubmitAppeal submitAppeal() {
        return new SubmitAppealUseCase(runResults, appeals, idGenerator, auditLog, clock);
    }

    public ResolveAppeal resolveAppeal() {
        return new ResolveAppealUseCase(appeals, runResults, rounds, rulebooks, auditLog, clock);
    }

    public RecalculateStandings recalculateStandings() {
        return new RecalculateStandingsUseCase(standings, rulebooks, scoreCollector, rankingService, auditLog, clock);
    }

    public CompetitionRepository competitions() {
        return competitions;
    }

    public TeamRegistrationRepository registrations() {
        return registrations;
    }

    public RoundRepository rounds() {
        return rounds;
    }

    public AppealRepository appeals() {
        return appeals;
    }

    public RunResultRepository runResults() {
        return runResults;
    }

    public StandingsRepository standings() {
        return standings;
    }

    public AuditLog auditLog() {
        return auditLog;
    }
}
