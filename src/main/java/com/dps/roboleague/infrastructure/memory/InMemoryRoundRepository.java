package com.dps.roboleague.infrastructure.memory;

import com.dps.roboleague.application.port.out.RoundRepository;
import com.dps.roboleague.domain.schedule.Round;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.RoundId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRoundRepository implements RoundRepository {

    private final Map<RoundId, Round> rounds = new LinkedHashMap<>();

    @Override
    public void save(Round round) {
        rounds.put(round.id(), round);
    }

    @Override
    public Optional<Round> findById(RoundId id) {
        return Optional.ofNullable(rounds.get(id));
    }

    @Override
    public List<Round> findByCompetition(CompetitionId competitionId) {
        return rounds.values().stream()
                .filter(round -> round.competitionId().equals(competitionId))
                .sorted(Comparator.comparingInt(Round::ordinal))
                .toList();
    }

    @Override
    public List<Round> findByCategory(CompetitionId competitionId, CategoryId categoryId) {
        return findByCompetition(competitionId).stream()
                .filter(round -> round.categoryId().equals(categoryId))
                .toList();
    }
}
