package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.List;
import java.util.Optional;

public interface StandingsRepository {

    /**
     * Guarda una revisión. Si ya existe una revisión con el mismo número para esa competencia y
     * categoría, la reemplaza: una revisión tiene un único estado vigente, provisional o definitiva.
     * Las revisiones anteriores no se tocan nunca. {@code StandingsRepositoryContractTest} fija este
     * contrato para cualquier adaptador.
     */
    void save(Standings standings);

    /** La revisión de número más alto, cualquiera sea su estado de publicación. */
    Optional<Standings> findLatest(CompetitionId competitionId, CategoryId categoryId);

    /** Todas las revisiones conservadas, de la más vieja a la más nueva. */
    List<Standings> findHistory(CompetitionId competitionId, CategoryId categoryId);
}
