package com.dps.roboleague.infrastructure.memory;

import com.dps.roboleague.application.port.out.CompetitionRepository;
import com.dps.roboleague.domain.competition.Competition;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCompetitionRepository implements CompetitionRepository {

    private final Map<CompetitionId, Competition> competitions = new HashMap<>();

    @Override
    public void save(Competition competition) {
        competitions.put(competition.id(), competition);
    }

    @Override
    public Optional<Competition> findById(CompetitionId id) {
        return Optional.ofNullable(competitions.get(id));
    }
}
