package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.List;

public interface GetStandings {

    Result execute(Command command);

    record Command(CompetitionId competitionId, CategoryId categoryId) {
    }

    /** La revisión vigente y todas las anteriores: publicar no borra el histórico. */
    record Result(Standings latest, List<Standings> history) {
    }
}
