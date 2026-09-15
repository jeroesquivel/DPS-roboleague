package com.dps.roboleague.infrastructure.memory;

import com.dps.roboleague.application.port.out.StandingsRepository;
import com.dps.roboleague.application.port.out.StandingsRepositoryContractTest;

class InMemoryStandingsRepositoryTest extends StandingsRepositoryContractTest {

    private final StandingsRepository repository = new InMemoryStandingsRepository();

    @Override
    protected StandingsRepository repository() {
        return repository;
    }
}
