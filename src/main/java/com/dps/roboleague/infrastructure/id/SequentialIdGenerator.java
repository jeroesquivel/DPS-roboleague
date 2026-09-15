package com.dps.roboleague.infrastructure.id;

import com.dps.roboleague.application.port.out.IdGenerator;
import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.HeatId;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.SeasonId;
import com.dps.roboleague.domain.shared.TeamId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class SequentialIdGenerator implements IdGenerator {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public SeasonId nextSeasonId() {
        return SeasonId.of(next("SEASON"));
    }

    @Override
    public CompetitionId nextCompetitionId() {
        return CompetitionId.of(next("COMPETITION"));
    }

    @Override
    public CategoryId nextCategoryId() {
        return CategoryId.of(next("CATEGORY"));
    }

    @Override
    public TeamId nextTeamId() {
        return TeamId.of(next("TEAM"));
    }

    @Override
    public RoundId nextRoundId() {
        return RoundId.of(next("ROUND"));
    }

    @Override
    public HeatId nextHeatId() {
        return HeatId.of(next("HEAT"));
    }

    @Override
    public RunId nextRunId() {
        return RunId.of(next("RUN"));
    }

    @Override
    public AppealId nextAppealId() {
        return AppealId.of(next("APPEAL"));
    }

    private String next(String prefix) {
        return prefix + "-" + counters.computeIfAbsent(prefix, key -> new AtomicInteger()).incrementAndGet();
    }
}
