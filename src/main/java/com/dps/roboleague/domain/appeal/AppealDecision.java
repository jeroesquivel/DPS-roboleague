package com.dps.roboleague.domain.appeal;

import com.dps.roboleague.domain.shared.DomainException;
import java.time.Instant;
import java.util.Objects;

public record AppealDecision(String reviewer, String rationale, Instant decidedAt) {

    public AppealDecision {
        Objects.requireNonNull(decidedAt, "decision timestamp is required");
        if (reviewer == null || reviewer.isBlank() || rationale == null || rationale.isBlank()) {
            throw new DomainException("an appeal decision requires a reviewer and a rationale");
        }
    }
}
