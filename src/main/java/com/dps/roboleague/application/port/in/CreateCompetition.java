package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.competition.RobotClass;
import com.dps.roboleague.domain.shared.AgeRange;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DateRange;
import com.dps.roboleague.domain.shared.SeasonId;
import java.util.List;

public interface CreateCompetition {

    CompetitionId execute(Command command);

    record Command(SeasonId seasonId, String name, DateRange period, List<CategoryDraft> categories, String actor) {
    }

    record CategoryDraft(String name, AgeRange ageRange, RobotClass robotClass) {
    }
}
