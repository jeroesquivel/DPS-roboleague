package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.schedule.Round;
import com.dps.roboleague.domain.shared.RoundId;

public interface FindRound {

    Round execute(RoundId roundId);
}
