package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.competition.Season;
import com.dps.roboleague.domain.shared.SeasonId;
import java.util.Optional;

public interface SeasonRepository {

    void save(Season season);

    Optional<Season> findById(SeasonId id);
}
