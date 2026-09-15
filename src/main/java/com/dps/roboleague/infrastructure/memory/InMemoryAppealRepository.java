package com.dps.roboleague.infrastructure.memory;

import com.dps.roboleague.application.port.out.AppealRepository;
import com.dps.roboleague.domain.appeal.Appeal;
import com.dps.roboleague.domain.shared.AppealId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryAppealRepository implements AppealRepository {

    private final Map<AppealId, Appeal> appeals = new LinkedHashMap<>();

    @Override
    public void save(Appeal appeal) {
        appeals.put(appeal.id(), appeal);
    }

    @Override
    public Optional<Appeal> findById(AppealId id) {
        return Optional.ofNullable(appeals.get(id));
    }
}
