package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.appeal.Appeal;
import com.dps.roboleague.domain.shared.AppealId;

public interface FindAppeal {

    Appeal execute(Command command);

    record Command(AppealId appealId) {
    }
}
