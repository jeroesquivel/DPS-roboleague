package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.CreateCompetition;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.CompetitionRepository;
import com.dps.roboleague.application.port.out.IdGenerator;
import com.dps.roboleague.application.port.out.SeasonRepository;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.competition.Category;
import com.dps.roboleague.domain.competition.Competition;
import com.dps.roboleague.domain.competition.Season;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.time.Clock;
import java.util.List;

public final class CreateCompetitionUseCase implements CreateCompetition {

    private final SeasonRepository seasons;
    private final CompetitionRepository competitions;
    private final IdGenerator idGenerator;
    private final AuditLog auditLog;
    private final Clock clock;

    public CreateCompetitionUseCase(SeasonRepository seasons, CompetitionRepository competitions,
            IdGenerator idGenerator, AuditLog auditLog, Clock clock) {
        this.seasons = seasons;
        this.competitions = competitions;
        this.idGenerator = idGenerator;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public Result execute(Command command) {
        Season season = seasons.findById(command.seasonId())
                .orElseThrow(() -> NotFoundException.of("Season", command.seasonId().value()));
        season.requireCompetitionPeriodInside(command.period());

        CompetitionId id = idGenerator.nextCompetitionId();
        List<Category> categories = command.categories().stream()
                .map(draft -> new Category(idGenerator.nextCategoryId(), draft.name(), draft.ageRange(),
                        draft.robotClass()))
                .toList();
        competitions.save(new Competition(id, season.id(), command.name(), command.period(), categories));
        auditLog.record(AuditEvent.of(clock.instant(), AuditAction.COMPETITION_CREATED, id.value(), command.actor()));
        return new Result(id, categories.stream().map(Category::id).toList());
    }
}
