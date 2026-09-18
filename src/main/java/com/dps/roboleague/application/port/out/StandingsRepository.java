package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.List;
import java.util.Optional;

public interface StandingsRepository {

    void save(Standings standings);

    Optional<Standings> findLatest(CompetitionId competitionId, CategoryId categoryId);

    List<Standings> findHistory(CompetitionId competitionId, CategoryId categoryId);
}
