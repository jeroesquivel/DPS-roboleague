package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.competition.RobotClass;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.AgeRange;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DateRange;
import java.util.List;
import java.util.Optional;

public interface FindCompetition {

    View execute(Command command);

    record Command(CompetitionId competitionId) {
    }

    record View(CompetitionId id, String name, DateRange period, Optional<RulebookVersion> activeRulebookVersion,
            List<CategoryView> categories) {

        public CategoryView firstCategory() {
            return categories.getFirst();
        }
    }

    record CategoryView(CategoryId id, String name, AgeRange ageRange, RobotClass robotClass) {
    }
}
