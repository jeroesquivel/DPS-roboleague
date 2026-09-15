package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.ResolveAppeal;
import com.dps.roboleague.application.port.out.AppealRepository;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.RoundRepository;
import com.dps.roboleague.application.port.out.RulebookRepository;
import com.dps.roboleague.application.port.out.RunResultRepository;
import com.dps.roboleague.domain.appeal.Appeal;
import com.dps.roboleague.domain.appeal.AppealDecision;
import com.dps.roboleague.domain.appeal.AppealStatus;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.result.ResultCorrection;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.schedule.Round;
import java.time.Clock;
import java.util.Map;

public final class ResolveAppealUseCase implements ResolveAppeal {

    private final AppealRepository appeals;
    private final RunResultRepository runResults;
    private final RoundRepository rounds;
    private final RulebookRepository rulebooks;
    private final AuditLog auditLog;
    private final Clock clock;

    public ResolveAppealUseCase(AppealRepository appeals, RunResultRepository runResults, RoundRepository rounds,
            RulebookRepository rulebooks, AuditLog auditLog, Clock clock) {
        this.appeals = appeals;
        this.runResults = runResults;
        this.rounds = rounds;
        this.rulebooks = rulebooks;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public AppealStatus execute(Command command) {
        Appeal appeal = appeals.findById(command.appealId())
                .orElseThrow(() -> NotFoundException.of("Appeal", command.appealId().value()));
        AppealDecision decision = new AppealDecision(command.reviewer(), command.rationale(), clock.instant());

        if (command.accepted()) {
            appeal.accept(decision);
            command.correction().ifPresent(correction -> correct(appeal, correction, command.actor()));
        } else {
            appeal.reject(decision);
        }
        appeals.save(appeal);

        auditLog.record(new AuditEvent(clock.instant(), AuditAction.APPEAL_RESOLVED, appeal.id().value(),
                command.actor(), Map.of("status", appeal.status().name(), "rationale", decision.rationale())));
        return appeal.status();
    }

    private void correct(Appeal appeal, Correction correction, String actor) {
        RunResult run = runResults.findById(appeal.runId())
                .orElseThrow(() -> NotFoundException.of("RunResult", appeal.runId().value()));
        Round round = rounds.findById(run.roundId())
                .orElseThrow(() -> NotFoundException.of("Round", run.roundId().value()));
        rulebooks.find(round.competitionId(), run.rulebookVersion())
                .orElseThrow(() -> NotFoundException.of("Rulebook", run.rulebookVersion().toString()))
                .challenge(run.challengeId())
                .validate(correction.measurements());

        String reason = "appeal " + appeal.id().value() + " accepted";
        run.applyCorrection(ResultCorrection.fromAppeal(appeal.id(), clock.instant(), actor, reason,
                correction.measurements(), correction.incidents()));
        runResults.save(run);

        auditLog.record(new AuditEvent(clock.instant(), AuditAction.RESULT_CORRECTED, run.id().value(), actor,
                Map.of("original", run.originalMeasurements().values().toString(),
                        "corrected", run.currentMeasurements().values().toString(),
                        "reason", reason)));
    }
}
