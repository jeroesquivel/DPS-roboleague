package com.dps.roboleague.infrastructure.config;

import com.dps.roboleague.application.port.in.CalculateRunScore;
import com.dps.roboleague.application.port.in.CaptureRunResult;
import com.dps.roboleague.application.port.in.CreateCompetition;
import com.dps.roboleague.application.port.in.CreateSeason;
import com.dps.roboleague.application.port.in.FindAppeal;
import com.dps.roboleague.application.port.in.FindAuditTrail;
import com.dps.roboleague.application.port.in.FindCompetition;
import com.dps.roboleague.application.port.in.FindRound;
import com.dps.roboleague.application.port.in.FindRunResult;
import com.dps.roboleague.application.port.in.FindTeamRegistration;
import com.dps.roboleague.application.port.in.GenerateStandings;
import com.dps.roboleague.application.port.in.GetStandings;
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
import com.dps.roboleague.application.service.CategoryScoringService;
import com.dps.roboleague.application.usecase.CalculateRunScoreUseCase;
import com.dps.roboleague.application.usecase.CaptureRunResultUseCase;
import com.dps.roboleague.application.usecase.CreateCompetitionUseCase;
import com.dps.roboleague.application.usecase.CreateSeasonUseCase;
import com.dps.roboleague.application.usecase.FindAppealUseCase;
import com.dps.roboleague.application.usecase.FindAuditTrailUseCase;
import com.dps.roboleague.application.usecase.FindCompetitionUseCase;
import com.dps.roboleague.application.usecase.FindRoundUseCase;
import com.dps.roboleague.application.usecase.FindRunResultUseCase;
import com.dps.roboleague.application.usecase.FindTeamRegistrationUseCase;
import com.dps.roboleague.application.usecase.GenerateStandingsUseCase;
import com.dps.roboleague.application.usecase.GetStandingsUseCase;
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

public final class RoboLeagueCompositionRoot {

    private final SeasonRepository seasons;
    private final CompetitionRepository competitions;
    private final RulebookRepository rulebooks;
    private final TeamRegistrationRepository registrations;
    private final RoundRepository rounds;
    private final RunResultRepository runResults;
    private final StandingsRepository standings;
    private final AppealRepository appeals;
    private final AuditLog auditLog;
    private final CategoryScoringService scoringService;
    private final RankingService rankingService = new RankingService();
    private final ScheduleConflictDetector conflictDetector = new ScheduleConflictDetector();
    private final IdGenerator idGenerator;
    private final Clock clock;

    public RoboLeagueCompositionRoot(SeasonRepository seasons, CompetitionRepository competitions,
            RulebookRepository rulebooks, TeamRegistrationRepository registrations, RoundRepository rounds,
            RunResultRepository runResults, StandingsRepository standings, AppealRepository appeals, AuditLog auditLog,
            IdGenerator idGenerator, Clock clock) {
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
        this.scoringService = new CategoryScoringService(rounds, runResults, rulebooks);
    }

    public static RoboLeagueCompositionRoot inMemory(Clock clock) {
        return new RoboLeagueCompositionRoot(new InMemorySeasonRepository(), new InMemoryCompetitionRepository(),
                new InMemoryRulebookRepository(), new InMemoryTeamRegistrationRepository(),
                new InMemoryRoundRepository(), new InMemoryRunResultRepository(), new InMemoryStandingsRepository(),
                new InMemoryAppealRepository(), new InMemoryAuditLog(), new SequentialIdGenerator(), clock);
    }


    public CreateSeason createSeasonUseCase() {
        return new CreateSeasonUseCase(seasons, idGenerator, auditLog, clock);
    }

    public CreateCompetition createCompetitionUseCase() {
        return new CreateCompetitionUseCase(seasons, competitions, idGenerator, auditLog, clock);
    }

    public PublishRulebook publishRulebookUseCase() {
        return new PublishRulebookUseCase(competitions, rulebooks, auditLog, clock);
    }

    public RegisterTeam registerTeamUseCase() {
        return new RegisterTeamUseCase(competitions, rulebooks, registrations, idGenerator, auditLog, clock);
    }

    public ScheduleRound scheduleRoundUseCase() {
        return new ScheduleRoundUseCase(competitions, rulebooks, registrations, rounds, conflictDetector, idGenerator,
                auditLog, clock);
    }

    public CaptureRunResult captureRunResultUseCase() {
        return new CaptureRunResultUseCase(rounds, rulebooks, runResults, idGenerator, auditLog, clock);
    }

    public GenerateStandings generateStandingsUseCase() {
        return new GenerateStandingsUseCase(competitions, rulebooks, standings, scoringService, rankingService,
                auditLog, clock);
    }

    public PublishStandings publishStandingsUseCase() {
        return new PublishStandingsUseCase(standings, auditLog, clock);
    }

    public SubmitAppeal submitAppealUseCase() {
        return new SubmitAppealUseCase(runResults, appeals, idGenerator, auditLog, clock);
    }

    public ResolveAppeal resolveAppealUseCase() {
        return new ResolveAppealUseCase(appeals, runResults, rounds, rulebooks, auditLog, clock);
    }

    public RecalculateStandings recalculateStandingsUseCase() {
        return new RecalculateStandingsUseCase(standings, rulebooks, scoringService, rankingService, auditLog, clock);
    }


    public CalculateRunScore calculateRunScoreUseCase() {
        return new CalculateRunScoreUseCase(runResults, rounds, scoringService);
    }

    public FindCompetition findCompetitionUseCase() {
        return new FindCompetitionUseCase(competitions);
    }

    public FindTeamRegistration findTeamRegistrationUseCase() {
        return new FindTeamRegistrationUseCase(registrations);
    }

    public FindRound findRoundUseCase() {
        return new FindRoundUseCase(rounds);
    }

    public FindRunResult findRunResultUseCase() {
        return new FindRunResultUseCase(runResults);
    }

    public FindAppeal findAppealUseCase() {
        return new FindAppealUseCase(appeals);
    }

    public GetStandings getStandingsUseCase() {
        return new GetStandingsUseCase(standings);
    }

    public FindAuditTrail findAuditTrailUseCase() {
        return new FindAuditTrailUseCase(auditLog);
    }
}
