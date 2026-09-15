package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.RecalculateStandings;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.RulebookRepository;
import com.dps.roboleague.application.port.out.StandingsRepository;
import com.dps.roboleague.application.service.CategoryScoreCollector;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.ranking.RankingService;
import com.dps.roboleague.domain.ranking.StandingEntry;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.rulebook.Rulebook;
import java.time.Clock;
import java.util.List;
import java.util.Map;

public final class RecalculateStandingsUseCase implements RecalculateStandings {

    private final StandingsRepository standings;
    private final RulebookRepository rulebooks;
    private final CategoryScoreCollector scoreCollector;
    private final RankingService rankingService;
    private final AuditLog auditLog;
    private final Clock clock;

    public RecalculateStandingsUseCase(StandingsRepository standings, RulebookRepository rulebooks,
            CategoryScoreCollector scoreCollector, RankingService rankingService, AuditLog auditLog, Clock clock) {
        this.standings = standings;
        this.rulebooks = rulebooks;
        this.scoreCollector = scoreCollector;
        this.rankingService = rankingService;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public Standings execute(Command command) {
        Standings current = standings.findLatest(command.competitionId(), command.categoryId())
                .orElseThrow(() -> NotFoundException.of("Standings", command.categoryId().value()));
        Rulebook rulebook = rulebooks.find(command.competitionId(), current.rulebookVersion())
                .orElseThrow(() -> NotFoundException.of("Rulebook", current.rulebookVersion().toString()));

        List<StandingEntry> entries = rankingService.rank(
                scoreCollector.collect(command.competitionId(), command.categoryId()), rulebook.tiebreakRules());
        Standings recalculated = current.supersede(entries, clock.instant());
        standings.save(recalculated);

        auditLog.record(new AuditEvent(clock.instant(), AuditAction.STANDINGS_RECALCULATED,
                command.categoryId().value(), command.actor(),
                Map.of("reason", command.reason(), "revision", String.valueOf(recalculated.revision()),
                        "rulebook", current.rulebookVersion().toString())));
        return recalculated;
    }
}
