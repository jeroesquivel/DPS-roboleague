package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.FindTeamRegistration;
import com.dps.roboleague.application.port.out.TeamRegistrationRepository;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.domain.team.TeamRegistration;

public final class FindTeamRegistrationUseCase implements FindTeamRegistration {

    private final TeamRegistrationRepository registrations;

    public FindTeamRegistrationUseCase(TeamRegistrationRepository registrations) {
        this.registrations = registrations;
    }

    @Override
    public TeamRegistration execute(TeamId teamId) {
        return registrations.findById(teamId)
                .orElseThrow(() -> NotFoundException.of("TeamRegistration", teamId.value()));
    }
}
