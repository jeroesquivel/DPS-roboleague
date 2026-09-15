package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.GenerateStandings;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.CompetitionRepository;
import com.dps.roboleague.application.port.out.RulebookRepository;
import com.dps.roboleague.application.port.out.StandingsRepository;
import com.dps.roboleague.application.service.CategoryScoreCollector;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.competition.Competition;
import com.dps.roboleague.domain.ranking.RankingService;
import com.dps.roboleague.domain.ranking.StandingEntry;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.rulebook.Rulebook;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.DomainException;
import java.time.Clock;
import java.util.List;
import java.util.Map;

public final class GenerateStandingsUseCase implements GenerateStandings {

    private final CompetitionRepository competitions;
    private final RulebookRepository rulebooks;
    private final StandingsRepository standings;
    private final CategoryScoreCollector scoreCollector;
    private final RankingService rankingService;
    private final AuditLog auditLog;
    private final Clock clock;

    public GenerateStandingsUseCase(CompetitionRepository competitions, RulebookRepository rulebooks,
            StandingsRepository standings, CategoryScoreCollector scoreCollector, RankingService rankingService,
            AuditLog auditLog, Clock clock) {
        this.competitions = competitions;
        this.rulebooks = rulebooks;
        this.standings = standings;
        this.scoreCollector = scoreCollector;
        this.rankingService = rankingService;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public Standings execute(Command command) {
        Competition competition = competitions.findById(command.competitionId())
                .orElseThrow(() -> NotFoundException.of("Competition", command.competitionId().value()));
        competition.category(command.categoryId());
        if (standings.findLatest(competition.id(), command.categoryId()).isPresent()) {
            throw new DomainException("standings already exist for this category, use a recalculation instead");
        }

        RulebookVersion version = competition.requireActiveRulebookVersion();
        Rulebook rulebook = rulebooks.find(competition.id(), version)
                .orElseThrow(() -> NotFoundException.of("Rulebook", version.toString()));
        List<StandingEntry> entries = rankingService.rank(
                scoreCollector.collect(competition.id(), command.categoryId()), rulebook.tiebreakRules());

        Standings generated = Standings.provisional(competition.id(), command.categoryId(), version, clock.instant(),
                entries);
        standings.save(generated);
        auditLog.record(new AuditEvent(clock.instant(), AuditAction.STANDINGS_GENERATED,
                command.categoryId().value(), command.actor(), Map.of("rulebook", version.toString())));
        return generated;
    }
}
