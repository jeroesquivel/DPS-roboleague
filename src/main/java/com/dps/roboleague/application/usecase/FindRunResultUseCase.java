package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.FindRunResult;
import com.dps.roboleague.application.port.out.RunResultRepository;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.shared.RunId;

public final class FindRunResultUseCase implements FindRunResult {

    private final RunResultRepository runResults;

    public FindRunResultUseCase(RunResultRepository runResults) {
        this.runResults = runResults;
    }

    @Override
    public RunResult execute(RunId runId) {
        return runResults.findById(runId)
                .orElseThrow(() -> NotFoundException.of("RunResult", runId.value()));
    }
}
