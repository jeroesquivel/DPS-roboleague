package com.dps.roboleague.infrastructure.memory;

import com.dps.roboleague.application.port.out.RunResultRepository;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRunResultRepository implements RunResultRepository {

    private final Map<RunId, RunResult> results = new LinkedHashMap<>();

    @Override
    public void save(RunResult result) {
        results.put(result.id(), result);
    }

    @Override
    public Optional<RunResult> findById(RunId id) {
        return Optional.ofNullable(results.get(id));
    }

    @Override
    public List<RunResult> findByRound(RoundId roundId) {
        return results.values().stream()
                .filter(result -> result.roundId().equals(roundId))
                .sorted(Comparator.comparingInt(RunResult::attemptNumber))
                .toList();
    }
}
