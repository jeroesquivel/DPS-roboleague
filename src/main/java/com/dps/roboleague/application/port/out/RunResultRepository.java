package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import java.util.List;
import java.util.Optional;

public interface RunResultRepository {

    void save(RunResult result);

    Optional<RunResult> findById(RunId id);

    List<RunResult> findByRound(RoundId roundId);
}
