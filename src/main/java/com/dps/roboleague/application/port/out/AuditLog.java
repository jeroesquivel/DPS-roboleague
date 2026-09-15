package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.audit.AuditEvent;
import java.util.List;

public interface AuditLog {

    void record(AuditEvent event);

    List<AuditEvent> findBySubject(String subject);
}
