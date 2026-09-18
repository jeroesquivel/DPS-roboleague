package com.dps.roboleague.infrastructure.memory;

import com.dps.roboleague.application.port.out.StandingsRepository;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryStandingsRepository implements StandingsRepository {

    private record Key(CompetitionId competitionId, CategoryId categoryId) {
    }

    private final Map<Key, List<Standings>> revisions = new HashMap<>();

    @Override
    public void save(Standings standings) {
        List<Standings> history = revisions.computeIfAbsent(
                new Key(standings.competitionId(), standings.categoryId()), key -> new ArrayList<>());
        history.removeIf(existing -> existing.revision() == standings.revision());
        history.add(standings);
    }

    @Override
    public Optional<Standings> findLatest(CompetitionId competitionId, CategoryId categoryId) {
        return findHistory(competitionId, categoryId).stream().max(Comparator.comparingInt(Standings::revision));
    }

    @Override
    public List<Standings> findHistory(CompetitionId competitionId, CategoryId categoryId) {
        return revisions.getOrDefault(new Key(competitionId, categoryId), List.of()).stream()
                .sorted(Comparator.comparingInt(Standings::revision))
                .toList();
    }
}
