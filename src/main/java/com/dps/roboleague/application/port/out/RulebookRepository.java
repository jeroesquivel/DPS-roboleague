package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.rulebook.Rulebook;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.Optional;

public interface RulebookRepository {

    void save(Rulebook rulebook);

    Optional<Rulebook> find(CompetitionId competitionId, RulebookVersion version);

    Optional<Rulebook> findLatest(CompetitionId competitionId);
}
