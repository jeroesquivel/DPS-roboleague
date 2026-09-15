package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.rulebook.Rulebook;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.Optional;

public interface RulebookRepository {

    /**
     * Guarda una versión del reglamento. Si esa versión ya existe la reemplaza; las versiones
     * anteriores se conservan siempre, porque son las que permiten recalcular un resultado con
     * exactamente las reglas bajo las que se corrió.
     */
    void save(Rulebook rulebook);

    Optional<Rulebook> find(CompetitionId competitionId, RulebookVersion version);

    Optional<Rulebook> findLatest(CompetitionId competitionId);
}
