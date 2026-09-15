package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;

public interface PublishStandings {

    Standings execute(Command command);

    record Command(CompetitionId competitionId, CategoryId categoryId, String actor) {
    }
}
