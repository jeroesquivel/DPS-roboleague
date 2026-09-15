package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.competition.Competition;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.Optional;

public interface CompetitionRepository {

    void save(Competition competition);

    Optional<Competition> findById(CompetitionId id);
}
