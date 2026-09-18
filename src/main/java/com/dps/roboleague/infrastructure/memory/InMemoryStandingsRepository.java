package com.dps.roboleague.infrastructure.memory;

import com.dps.roboleague.application.port.out.StandingsRepository;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public final class InMemoryStandingsRepository implements StandingsRepository {

    private record Key(CompetitionId competitionId, CategoryId categoryId) {
    }

    private final Map<Key, NavigableMap<Integer, Standings>> revisions = new HashMap<>();

    @Override
    public void save(Standings standings) {
        revisions.computeIfAbsent(new Key(standings.competitionId(), standings.categoryId()), key -> new TreeMap<>())
                .put(standings.revision(), standings);
    }

    @Override
    public Optional<Standings> findLatest(CompetitionId competitionId, CategoryId categoryId) {
        NavigableMap<Integer, Standings> history = revisions.get(new Key(competitionId, categoryId));
        return history == null ? Optional.empty() : Optional.of(history.lastEntry().getValue());
    }

    @Override
    public List<Standings> findHistory(CompetitionId competitionId, CategoryId categoryId) {
        NavigableMap<Integer, Standings> history = revisions.get(new Key(competitionId, categoryId));
        return history == null ? List.of() : List.copyOf(history.values());
    }
}
