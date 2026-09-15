package com.dps.roboleague.application.port.out;

import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.domain.team.TeamRegistration;
import java.util.Optional;

public interface TeamRegistrationRepository {

    void save(TeamRegistration registration);

    Optional<TeamRegistration> findById(TeamId id);
}
