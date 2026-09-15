package com.dps.roboleague.infrastructure.memory;

import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.domain.audit.AuditEvent;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryAuditLog implements AuditLog {

    private final List<AuditEvent> events = new ArrayList<>();

    @Override
    public void record(AuditEvent event) {
        events.add(event);
    }

    @Override
    public List<AuditEvent> findBySubject(String subject) {
        return events.stream().filter(event -> event.subject().equals(subject)).toList();
    }
}
