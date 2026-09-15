package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.GetStandings;
import com.dps.roboleague.application.port.out.StandingsRepository;
import com.dps.roboleague.domain.ranking.Standings;

public final class GetStandingsUseCase implements GetStandings {

    private final StandingsRepository standings;

    public GetStandingsUseCase(StandingsRepository standings) {
        this.standings = standings;
    }

    @Override
    public Result execute(Command command) {
        Standings latest = standings.findLatest(command.competitionId(), command.categoryId())
                .orElseThrow(() -> NotFoundException.of("Standings", command.categoryId().value()));
        return new Result(latest, standings.findHistory(command.competitionId(), command.categoryId()));
    }
}
