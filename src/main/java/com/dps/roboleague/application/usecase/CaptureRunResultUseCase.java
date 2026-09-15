package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.CaptureRunResult;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.IdGenerator;
import com.dps.roboleague.application.port.out.RoundRepository;
import com.dps.roboleague.application.port.out.RulebookRepository;
import com.dps.roboleague.application.port.out.RunResultRepository;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.challenge.ChallengeSpec;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.schedule.Heat;
import com.dps.roboleague.domain.schedule.Round;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.RunId;
import java.time.Clock;
import java.util.Map;

public final class CaptureRunResultUseCase implements CaptureRunResult {

    private final RoundRepository rounds;
    private final RulebookRepository rulebooks;
    private final RunResultRepository runResults;
    private final IdGenerator idGenerator;
    private final AuditLog auditLog;
    private final Clock clock;

    public CaptureRunResultUseCase(RoundRepository rounds, RulebookRepository rulebooks,
            RunResultRepository runResults, IdGenerator idGenerator, AuditLog auditLog, Clock clock) {
        this.rounds = rounds;
        this.rulebooks = rulebooks;
        this.runResults = runResults;
        this.idGenerator = idGenerator;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public RunId execute(Command command) {
        Round round = rounds.findById(command.roundId())
                .orElseThrow(() -> NotFoundException.of("Round", command.roundId().value()));
        Heat heat = round.heatFor(command.teamId())
                .orElseThrow(() -> new DomainException("team " + command.teamId().value()
                        + " has no heat in round " + round.id().value()));

        ChallengeSpec challenge = rulebooks.find(round.competitionId(), round.rulebookVersion())
                .orElseThrow(() -> NotFoundException.of("Rulebook", round.rulebookVersion().toString()))
                .challenge(round.challengeId());
        challenge.requireAttemptWithinLimit(command.attemptNumber());
        challenge.validate(command.measurements());
        challenge.validateIncidents(command.incidents());
        requireUnusedAttempt(command);

        RunId runId = idGenerator.nextRunId();
        RunResult result = new RunResult(runId, round.id(), heat.id(), command.teamId(), round.challengeId(),
                round.rulebookVersion(), command.attemptNumber(), clock.instant(), command.measurements(),
                command.evaluations(), command.incidents());
        runResults.save(result);

        auditLog.record(new AuditEvent(clock.instant(), AuditAction.RESULT_CAPTURED, runId.value(), command.actor(),
                Map.of("measurements", command.measurements().values().toString(),
                        "rulebook", round.rulebookVersion().toString())));
        return runId;
    }

    private void requireUnusedAttempt(Command command) {
        boolean alreadyCaptured = runResults.findByRound(command.roundId()).stream()
                .anyMatch(run -> run.teamId().equals(command.teamId())
                        && run.attemptNumber() == command.attemptNumber());
        if (alreadyCaptured) {
            throw new DomainException("attempt " + command.attemptNumber() + " of team "
                    + command.teamId().value() + " was already captured");
        }
    }
}
