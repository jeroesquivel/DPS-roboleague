package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.shared.DateRange;
import com.dps.roboleague.domain.shared.SeasonId;

public interface CreateSeason {

    SeasonId execute(Command command);

    record Command(String name, int year, DateRange period, String actor) {
    }
}
