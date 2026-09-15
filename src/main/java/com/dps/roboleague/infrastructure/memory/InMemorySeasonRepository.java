package com.dps.roboleague.infrastructure.memory;

import com.dps.roboleague.application.port.out.SeasonRepository;
import com.dps.roboleague.domain.competition.Season;
import com.dps.roboleague.domain.shared.SeasonId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemorySeasonRepository implements SeasonRepository {

    private final Map<SeasonId, Season> seasons = new HashMap<>();

    @Override
    public void save(Season season) {
        seasons.put(season.id(), season);
    }

    @Override
    public Optional<Season> findById(SeasonId id) {
        return Optional.ofNullable(seasons.get(id));
    }
}
