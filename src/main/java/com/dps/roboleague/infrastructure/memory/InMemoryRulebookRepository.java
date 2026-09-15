package com.dps.roboleague.infrastructure.memory;

import com.dps.roboleague.application.port.out.RulebookRepository;
import com.dps.roboleague.domain.rulebook.Rulebook;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryRulebookRepository implements RulebookRepository {

    private final Map<CompetitionId, List<Rulebook>> rulebooks = new HashMap<>();

    @Override
    public void save(Rulebook rulebook) {
        List<Rulebook> versions = rulebooks.computeIfAbsent(rulebook.competitionId(), key -> new ArrayList<>());
        versions.removeIf(existing -> existing.version().equals(rulebook.version()));
        versions.add(rulebook);
    }

    @Override
    public Optional<Rulebook> find(CompetitionId competitionId, RulebookVersion version) {
        return versionsOf(competitionId).stream()
                .filter(rulebook -> rulebook.version().equals(version))
                .findFirst();
    }

    @Override
    public Optional<Rulebook> findLatest(CompetitionId competitionId) {
        return versionsOf(competitionId).stream().max(Comparator.comparing(Rulebook::version));
    }

    private List<Rulebook> versionsOf(CompetitionId competitionId) {
        return rulebooks.getOrDefault(competitionId, List.of());
    }
}
