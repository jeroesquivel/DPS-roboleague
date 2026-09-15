package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.port.in.FindAuditTrail;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.domain.audit.AuditEvent;
import java.util.List;

public final class FindAuditTrailUseCase implements FindAuditTrail {

    private final AuditLog auditLog;

    public FindAuditTrailUseCase(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public List<AuditEvent> execute(Command command) {
        return auditLog.findBySubject(command.subject());
    }
}
