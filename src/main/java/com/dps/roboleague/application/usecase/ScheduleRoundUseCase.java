package com.dps.roboleague.application.usecase;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.ScheduleRound;
import com.dps.roboleague.application.port.out.AuditLog;
import com.dps.roboleague.application.port.out.CompetitionRepository;
import com.dps.roboleague.application.port.out.IdGenerator;
import com.dps.roboleague.application.port.out.RoundRepository;
import com.dps.roboleague.application.port.out.RulebookRepository;
import com.dps.roboleague.application.port.out.TeamRegistrationRepository;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.competition.Competition;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.schedule.Heat;
import com.dps.roboleague.domain.schedule.Round;
import com.dps.roboleague.domain.schedule.ScheduleConflict;
import com.dps.roboleague.domain.schedule.ScheduleConflictDetector;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.team.TeamRegistration;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ScheduleRoundUseCase implements ScheduleRound {

    private final CompetitionRepository competitions;
    private final RulebookRepository rulebooks;
    private final TeamRegistrationRepository registrations;
    private final RoundRepository rounds;
    private final ScheduleConflictDetector conflictDetector;
    private final IdGenerator idGenerator;
    private final AuditLog auditLog;
    private final Clock clock;

    public ScheduleRoundUseCase(CompetitionRepository competitions, RulebookRepository rulebooks,
            TeamRegistrationRepository registrations, RoundRepository rounds,
            ScheduleConflictDetector conflictDetector, IdGenerator idGenerator, AuditLog auditLog, Clock clock) {
        this.competitions = competitions;
        this.rulebooks = rulebooks;
        this.registrations = registrations;
        this.rounds = rounds;
        this.conflictDetector = conflictDetector;
        this.idGenerator = idGenerator;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Override
    public RoundId execute(Command command) {
        Competition competition = competitions.findById(command.competitionId())
                .orElseThrow(() -> NotFoundException.of("Competition", command.competitionId().value()));
        competition.category(command.categoryId());
        RulebookVersion version = competition.requireActiveRulebookVersion();
        rulebooks.find(competition.id(), version)
                .orElseThrow(() -> NotFoundException.of("Rulebook", version.toString()))
                .challenge(command.challengeId());

        RoundId roundId = idGenerator.nextRoundId();
        Round round = new Round(roundId, competition.id(), command.categoryId(), command.challengeId(),
                command.ordinal(), version);
        List<Heat> booked = new ArrayList<>(bookedHeats(competition));

        for (HeatDraft draft : command.heats()) {
            requireEligibleTeam(command, draft);
            requireSlotWithinCompetition(competition, draft);
            Heat heat = new Heat(idGenerator.nextHeatId(), roundId, draft.teamId(), draft.arenaId(), draft.slot(),
                    draft.judges());
            List<ScheduleConflict> conflicts = conflictDetector.detect(booked, heat);
            if (!conflicts.isEmpty()) {
                throw new DomainException("the heat cannot be scheduled: " + describe(conflicts));
            }
            round.schedule(heat);
            booked.add(heat);
        }

        rounds.save(round);
        auditLog.record(new AuditEvent(clock.instant(), AuditAction.ROUND_SCHEDULED, roundId.value(), command.actor(),
                Map.of("heats", String.valueOf(round.heats().size()), "rulebook", version.toString())));
        return roundId;
    }

    private List<Heat> bookedHeats(Competition competition) {
        return rounds.findByCompetition(competition.id()).stream()
                .flatMap(scheduled -> scheduled.heats().stream())
                .toList();
    }

    private void requireEligibleTeam(Command command, HeatDraft draft) {
        TeamRegistration registration = registrations.findById(draft.teamId())
                .orElseThrow(() -> NotFoundException.of("TeamRegistration", draft.teamId().value()));
        if (!registration.isAccepted()) {
            throw new DomainException("team " + registration.name() + " is not accepted in the competition");
        }
        if (!registration.categoryId().equals(command.categoryId())) {
            throw new DomainException("team " + registration.name() + " does not compete in the scheduled category");
        }
    }

    private void requireSlotWithinCompetition(Competition competition, HeatDraft draft) {
        competition.requireDateWithinPeriod(draft.slot().start().toLocalDate());
        competition.requireDateWithinPeriod(draft.slot().end().toLocalDate());
    }

    private String describe(List<ScheduleConflict> conflicts) {
        return conflicts.stream()
                .map(conflict -> conflict.type() + " (" + conflict.detail() + ")")
                .collect(Collectors.joining(", "));
    }
}
