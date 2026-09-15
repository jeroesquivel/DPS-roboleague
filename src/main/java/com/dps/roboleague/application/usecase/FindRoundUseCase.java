package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.FindRound;
import com.dps.roboleague.application.port.out.RoundRepository;
import com.dps.roboleague.domain.schedule.Round;

public final class FindRoundUseCase implements FindRound {

    private final RoundRepository rounds;

    public FindRoundUseCase(RoundRepository rounds) {
        this.rounds = rounds;
    }

    @Override
    public Round execute(Command command) {
        return rounds.findById(command.roundId())
                .orElseThrow(() -> NotFoundException.of("Round", command.roundId().value()));
    }
}
