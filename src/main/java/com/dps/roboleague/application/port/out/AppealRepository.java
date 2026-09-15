package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.appeal.Appeal;
import com.dps.roboleague.domain.shared.AppealId;
import java.util.Optional;

public interface AppealRepository {

    void save(Appeal appeal);

    Optional<Appeal> findById(AppealId id);
}
