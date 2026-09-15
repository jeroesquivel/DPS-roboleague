package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.CalculateRunScore;
import com.dps.roboleague.application.port.out.RoundRepository;
import com.dps.roboleague.application.port.out.RunResultRepository;
import com.dps.roboleague.application.service.CategoryScoreCollector;
import com.dps.roboleague.domain.ranking.ScoredRun;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.schedule.Round;

public final class CalculateRunScoreUseCase implements CalculateRunScore {

    private final RunResultRepository runResults;
    private final RoundRepository rounds;
    private final CategoryScoreCollector scoreCollector;

    public CalculateRunScoreUseCase(RunResultRepository runResults, RoundRepository rounds,
            CategoryScoreCollector scoreCollector) {
        this.runResults = runResults;
        this.rounds = rounds;
        this.scoreCollector = scoreCollector;
    }

    @Override
    public RunScore execute(Command command) {
        RunResult run = runResults.findById(command.runId())
                .orElseThrow(() -> NotFoundException.of("RunResult", command.runId().value()));
        Round round = rounds.findById(run.roundId())
                .orElseThrow(() -> NotFoundException.of("Round", run.roundId().value()));
        ScoredRun scored = scoreCollector.scoreRun(run, round.competitionId());
        return new RunScore(run.id(), run.teamId(), run.challengeId(), run.rulebookVersion(), scored.breakdown());
    }
}
