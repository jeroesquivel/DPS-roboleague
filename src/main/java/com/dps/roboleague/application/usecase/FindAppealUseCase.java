package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.FindAppeal;
import com.dps.roboleague.application.port.out.AppealRepository;
import com.dps.roboleague.domain.appeal.Appeal;

public final class FindAppealUseCase implements FindAppeal {

    private final AppealRepository appeals;

    public FindAppealUseCase(AppealRepository appeals) {
        this.appeals = appeals;
    }

    @Override
    public Appeal execute(Command command) {
        return appeals.findById(command.appealId())
                .orElseThrow(() -> NotFoundException.of("Appeal", command.appealId().value()));
    }
}
