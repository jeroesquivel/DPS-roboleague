package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.FindCompetition;
import com.dps.roboleague.application.port.out.CompetitionRepository;
import com.dps.roboleague.domain.competition.Category;
import com.dps.roboleague.domain.competition.Competition;

public final class FindCompetitionUseCase implements FindCompetition {

    private final CompetitionRepository competitions;

    public FindCompetitionUseCase(CompetitionRepository competitions) {
        this.competitions = competitions;
    }

    @Override
    public View execute(Command command) {
        Competition competition = competitions.findById(command.competitionId())
                .orElseThrow(() -> NotFoundException.of("Competition", command.competitionId().value()));
        return new View(competition.id(), competition.name(), competition.period(),
                competition.activeRulebookVersion(),
                competition.categories().stream().map(FindCompetitionUseCase::viewOf).toList());
    }

    private static CategoryView viewOf(Category category) {
        return new CategoryView(category.id(), category.name(), category.ageRange(), category.robotClass());
    }
}
