package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.PublishRulebook;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.CompetitionRepository;
import com.dps.roboleague.application.port.out.RulebookRepository;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.competition.Competition;
import com.dps.roboleague.domain.rulebook.Rulebook;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

public final class PublishRulebookUseCase implements PublishRulebook {

    private final CompetitionRepository competitions;
    private final RulebookRepository rulebooks;
    private final AuditLog auditLog;
    private final Clock clock;

    public PublishRulebookUseCase(CompetitionRepository competitions, RulebookRepository rulebooks, AuditLog auditLog,
            Clock clock) {
        this.competitions = competitions;
        this.rulebooks = rulebooks;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public RulebookVersion execute(Command command) {
        Competition competition = competitions.findById(command.competitionId())
                .orElseThrow(() -> NotFoundException.of("Competition", command.competitionId().value()));

        RulebookVersion version = rulebooks.findLatest(competition.id())
                .map(rulebook -> rulebook.version().next())
                .orElseGet(RulebookVersion::first);

        Rulebook.Builder builder = Rulebook.builder(competition.id(), version, LocalDate.now(clock))
                .withEligibilityPolicy(command.eligibilityPolicy());
        command.challenges().forEach(builder::withChallenge);
        command.tiebreakRules().forEach(builder::withTiebreak);

        rulebooks.save(builder.build());
        competition.activateRulebook(version);
        competitions.save(competition);

        auditLog.record(new AuditEvent(clock.instant(), AuditAction.RULEBOOK_PUBLISHED, competition.id().value(),
                command.actor(), Map.of("version", version.toString())));
        return version;
    }
}
