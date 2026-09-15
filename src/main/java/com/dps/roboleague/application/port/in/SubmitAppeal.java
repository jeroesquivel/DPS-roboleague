package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.shared.AppealId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;

public interface SubmitAppeal {

    AppealId execute(Command command);

    record Command(RunId runId, TeamId teamId, String claim, String actor) {
    }
}
