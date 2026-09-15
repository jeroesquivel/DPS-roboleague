package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.RegisterTeam;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.CompetitionRepository;
import com.dps.roboleague.application.port.out.IdGenerator;
import com.dps.roboleague.application.port.out.RulebookRepository;
import com.dps.roboleague.application.port.out.TeamRegistrationRepository;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.competition.Category;
import com.dps.roboleague.domain.competition.Competition;
import com.dps.roboleague.domain.eligibility.EligibilityRequest;
import com.dps.roboleague.domain.eligibility.EligibilityVerdict;
import com.dps.roboleague.domain.rulebook.Rulebook;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.team.TeamRegistration;
import java.time.Clock;
import java.util.Map;

public final class RegisterTeamUseCase implements RegisterTeam {

    private final CompetitionRepository competitions;
    private final RulebookRepository rulebooks;
    private final TeamRegistrationRepository registrations;
    private final IdGenerator idGenerator;
    private final AuditLog auditLog;
    private final Clock clock;

    public RegisterTeamUseCase(CompetitionRepository competitions, RulebookRepository rulebooks,
            TeamRegistrationRepository registrations, IdGenerator idGenerator, AuditLog auditLog, Clock clock) {
        this.competitions = competitions;
        this.rulebooks = rulebooks;
        this.registrations = registrations;
        this.idGenerator = idGenerator;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public Outcome execute(Command command) {
        Competition competition = competitions.findById(command.competitionId())
                .orElseThrow(() -> NotFoundException.of("Competition", command.competitionId().value()));
        Category category = competition.category(command.categoryId());
        RulebookVersion version = competition.requireActiveRulebookVersion();
        Rulebook rulebook = rulebooks.find(competition.id(), version)
                .orElseThrow(() -> NotFoundException.of("Rulebook", version.toString()));

        TeamRegistration registration = new TeamRegistration(idGenerator.nextTeamId(), competition.id(),
                category.id(), command.teamName(), command.members(), command.robot(), command.documents());

        EligibilityVerdict verdict = rulebook.eligibilityPolicy()
                .verdictFor(new EligibilityRequest(registration, category, competition.period().start()));
        if (verdict.isEligible()) {
            registration.accept();
        } else {
            registration.reject(verdict.reasons());
        }
        registrations.save(registration);
        auditLog.record(auditEventFor(registration, verdict, version, command.actor()));
        return new Outcome(registration.id(), registration.status(), verdict);
    }

    private AuditEvent auditEventFor(TeamRegistration registration, EligibilityVerdict verdict,
            RulebookVersion version, String actor) {
        AuditAction action = verdict.isEligible() ? AuditAction.TEAM_REGISTERED : AuditAction.TEAM_REJECTED;
        Map<String, String> details = Map.of("rulebook", version.toString(),
                "violations", String.join(" | ", verdict.reasons()));
        return new AuditEvent(clock.instant(), action, registration.id().value(), actor, details);
    }
}
