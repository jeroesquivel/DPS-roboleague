package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.SubmitAppeal;
import com.dps.roboleague.application.port.out.AppealRepository;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.IdGenerator;
import com.dps.roboleague.application.port.out.RunResultRepository;
import com.dps.roboleague.domain.appeal.Appeal;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.DomainException;
import java.time.Clock;
import java.util.Map;

public final class SubmitAppealUseCase implements SubmitAppeal {

    private final RunResultRepository runResults;
    private final AppealRepository appeals;
    private final IdGenerator idGenerator;
    private final AuditLog auditLog;
    private final Clock clock;

    public SubmitAppealUseCase(RunResultRepository runResults, AppealRepository appeals, IdGenerator idGenerator,
            AuditLog auditLog, Clock clock) {
        this.runResults = runResults;
        this.appeals = appeals;
        this.idGenerator = idGenerator;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public AppealId execute(Command command) {
        RunResult run = runResults.findById(command.runId())
                .orElseThrow(() -> NotFoundException.of("RunResult", command.runId().value()));
        if (!run.teamId().equals(command.teamId())) {
            throw new DomainException("team " + command.teamId().value() + " cannot appeal a run of another team");
        }

        AppealId appealId = AppealId.of(idGenerator.nextId("APPEAL"));
        appeals.save(new Appeal(appealId, run.id(), command.teamId(), command.claim(), clock.instant()));
        auditLog.record(new AuditEvent(clock.instant(), AuditAction.APPEAL_SUBMITTED, appealId.value(),
                command.actor(), Map.of("run", run.id().value())));
        return appealId;
    }
}
