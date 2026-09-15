package com.dps.roboleague.domain.appeal;

import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class Appeal {

    private final AppealId id;
    private final RunId runId;
    private final TeamId teamId;
    private final String claim;
    private final Instant submittedAt;
    private AppealStatus status = AppealStatus.SUBMITTED;
    private AppealDecision decision;

    public Appeal(AppealId id, RunId runId, TeamId teamId, String claim, Instant submittedAt) {
        this.id = Objects.requireNonNull(id, "appeal id is required");
        this.runId = Objects.requireNonNull(runId, "run id is required");
        this.teamId = Objects.requireNonNull(teamId, "team id is required");
        this.submittedAt = Objects.requireNonNull(submittedAt, "submission timestamp is required");
        if (claim == null || claim.isBlank()) {
            throw new DomainException("an appeal requires a claim");
        }
        this.claim = claim;
    }

    public void accept(AppealDecision decision) {
        resolveWith(decision, AppealStatus.ACCEPTED);
    }

    public void reject(AppealDecision decision) {
        resolveWith(decision, AppealStatus.REJECTED);
    }

    public boolean isAccepted() {
        return status == AppealStatus.ACCEPTED;
    }

    private void resolveWith(AppealDecision newDecision, AppealStatus newStatus) {
        Objects.requireNonNull(newDecision, "decision is required");
        if (status != AppealStatus.SUBMITTED) {
            throw new DomainException("appeal " + id.value() + " was already resolved as " + status);
        }
        if (newDecision.decidedAt().isBefore(submittedAt)) {
            throw new DomainException("a decision cannot predate the submission of appeal " + id.value());
        }
        this.decision = newDecision;
        this.status = newStatus;
    }

    public AppealId id() {
        return id;
    }

    public RunId runId() {
        return runId;
    }

    public TeamId teamId() {
        return teamId;
    }

    public String claim() {
        return claim;
    }

    public Instant submittedAt() {
        return submittedAt;
    }

    public AppealStatus status() {
        return status;
    }

    public Optional<AppealDecision> decision() {
        return Optional.ofNullable(decision);
    }
}
