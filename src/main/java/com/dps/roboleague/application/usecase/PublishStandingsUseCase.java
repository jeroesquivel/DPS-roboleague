package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.PublishStandings;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.StandingsRepository;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.ranking.Standings;
import java.time.Clock;
import java.util.Map;

public final class PublishStandingsUseCase implements PublishStandings {

    private final StandingsRepository standings;
    private final AuditLog auditLog;
    private final Clock clock;

    public PublishStandingsUseCase(StandingsRepository standings, AuditLog auditLog, Clock clock) {
        this.standings = standings;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public Standings execute(Command command) {
        Standings provisional = standings.findLatest(command.competitionId(), command.categoryId())
                .orElseThrow(() -> NotFoundException.of("Standings", command.categoryId().value()));
        Standings published = provisional.publish();
        standings.save(published);
        auditLog.record(new AuditEvent(clock.instant(), AuditAction.STANDINGS_PUBLISHED,
                command.categoryId().value(), command.actor(),
                Map.of("revision", String.valueOf(published.revision()))));
        return published;
    }
}
