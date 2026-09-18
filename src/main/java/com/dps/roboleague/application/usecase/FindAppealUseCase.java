package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.FindAppeal;
import com.dps.roboleague.application.port.out.AppealRepository;
import com.dps.roboleague.domain.appeal.Appeal;
import com.dps.roboleague.domain.shared.AppealId;

public final class FindAppealUseCase implements FindAppeal {

    private final AppealRepository appeals;

    public FindAppealUseCase(AppealRepository appeals) {
        this.appeals = appeals;
    }

    @Override
    public Appeal execute(AppealId appealId) {
        return appeals.findById(appealId)
                .orElseThrow(() -> NotFoundException.of("Appeal", appealId.value()));
    }
}
