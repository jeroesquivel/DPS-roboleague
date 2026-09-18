package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.audit.AuditEvent;
import java.util.List;

public interface FindAuditTrail {

    List<AuditEvent> execute(Command command);

    record Command(String subject) {
    }
}
