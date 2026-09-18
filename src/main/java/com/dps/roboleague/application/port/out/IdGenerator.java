package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.HeatId;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.SeasonId;
import com.dps.roboleague.domain.shared.TeamId;

public interface IdGenerator {

    SeasonId nextSeasonId();

    CompetitionId nextCompetitionId();

    CategoryId nextCategoryId();

    TeamId nextTeamId();

    RoundId nextRoundId();

    HeatId nextHeatId();

    RunId nextRunId();

    AppealId nextAppealId();
}
