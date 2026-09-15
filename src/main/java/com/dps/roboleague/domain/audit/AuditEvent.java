package com.dps.roboleague.domain.audit;

import com.dps.roboleague.domain.shared.DomainException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AuditEvent(Instant occurredAt, AuditAction action, String subject, String actor,
        Map<String, String> details) {

    public AuditEvent {
        Objects.requireNonNull(occurredAt, "audit timestamp is required");
        Objects.requireNonNull(action, "audit action is required");
        if (subject == null || subject.isBlank() || actor == null || actor.isBlank()) {
            throw new DomainException("an audit event requires a subject and an actor");
        }
        details = Map.copyOf(details);
    }

    public static AuditEvent of(Instant occurredAt, AuditAction action, String subject, String actor) {
        return new AuditEvent(occurredAt, action, subject, actor, Map.of());
    }
}
