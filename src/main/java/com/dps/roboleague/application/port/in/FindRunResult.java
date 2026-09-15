package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.shared.RunId;

public interface FindRunResult {

    RunResult execute(Command command);

    record Command(RunId runId) {
    }
}
