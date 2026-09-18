package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.port.in.ScheduleRound;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.schedule.Round;
import com.dps.roboleague.domain.schedule.ScheduleConflictType;
import com.dps.roboleague.domain.schedule.TimeSlot;
import com.dps.roboleague.domain.shared.ArenaId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.JudgeId;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.support.TeamFixtures;
import com.dps.roboleague.support.TestEdition;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScheduleRoundUseCaseTest {

    private static final LocalDateTime TEN = LocalDateTime.of(2026, 3, 2, 10, 0);

    private final TestEdition edition = TestEdition.start();

    @Test
    void schedulesOneHeatPerTeamWithItsArenaAndJudges() {
        TeamId delta = edition.registerEligibleTeam("Delta Bots");
        TeamId omega = edition.registerEligibleTeam("Omega Crew");

        RoundId roundId = edition.scheduleRoundFor(1, List.of(delta, omega));

        Round round = edition.round(roundId);

        assertEquals(2, round.heats().size());
        assertEquals(RulebookVersion.first(), round.rulebookVersion());
        assertTrue(round.heatFor(delta).isPresent());
        assertEquals(2, round.heatFor(omega).orElseThrow().judges().size());
    }

    @Test
    void reusesTheTeamArenaAndJudgesWhenThePreviousHeatEnds() {
        TeamId delta = edition.registerEligibleTeam("Delta Bots");
        RoundId first = edition.scheduleRound(1, List.of(edition.heat(delta, "A1", TEN)));

        RoundId next = edition.scheduleRound(2, List.of(edition.heat(delta, "A1", TEN.plusMinutes(15))));

        assertEquals(edition.round(first).heatFor(delta).orElseThrow().slot().end(),
                edition.round(next).heatFor(delta).orElseThrow().slot().start());
        assertEquals(1, edition.round(first).heats().size());
        assertEquals(1, edition.round(next).heats().size());
    }

    @Test
    void schedulesSimultaneousHeatsWithIndependentTeamsArenasAndJudges() {
        TeamId delta = edition.registerEligibleTeam("Delta Bots");
        TeamId omega = edition.registerEligibleTeam("Omega Crew");
        ScheduleRound.HeatDraft omegaHeat = new ScheduleRound.HeatDraft(omega, ArenaId.of("A2"),
                new TimeSlot(TEN, Duration.ofMinutes(15)), Set.of(JudgeId.of("J3")));

        RoundId roundId = edition.scheduleRound(1, List.of(edition.heat(delta, "A1", TEN), omegaHeat));

        Round round = edition.round(roundId);
        assertEquals(2, round.heats().size());
        assertEquals(round.heatFor(delta).orElseThrow().slot(), round.heatFor(omega).orElseThrow().slot());
        assertEquals(ArenaId.of("A2"), round.heatFor(omega).orElseThrow().arenaId());
        assertEquals(Set.of(JudgeId.of("J3")), round.heatFor(omega).orElseThrow().judges());
    }

    @Test
    void rejectsConflictingHeatsInOneRequestWithoutKeepingAPartialRound() {
        TeamId delta = edition.registerEligibleTeam("Delta Bots");
        TeamId omega = edition.registerEligibleTeam("Omega Crew");

        assertThrows(DomainException.class, () -> edition.scheduleRound(1,
                List.of(edition.heat(delta, "A1", TEN), edition.heat(omega, "A1", TEN.plusMinutes(5)))));

        RoundId retried = edition.scheduleRound(1, List.of(edition.heat(delta, "A1", TEN)));
        assertEquals(1, edition.round(retried).heats().size());
        assertEquals(delta, edition.round(retried).heats().getFirst().teamId());
    }

    @Test
    void rejectsAHeatThatReusesAnArenaAlreadyBooked() {
        TeamId delta = edition.registerEligibleTeam("Delta Bots");
        TeamId omega = edition.registerEligibleTeam("Omega Crew");
        edition.scheduleRound(1, List.of(edition.heat(delta, "A1", TEN)));

        DomainException error = assertThrows(DomainException.class,
                () -> edition.scheduleRound(2, List.of(edition.heat(omega, "A1", TEN.plusMinutes(5)))));

        assertTrue(error.getMessage().contains(ScheduleConflictType.ARENA_BUSY.name()));
    }

    @Test
    void rejectsAHeatScheduledOutsideThePeriodOfTheCompetition() {
        TeamId delta = edition.registerEligibleTeam("Delta Bots");

        DomainException error = assertThrows(DomainException.class, () -> edition.scheduleRound(1,
                List.of(edition.heat(delta, "A1", LocalDateTime.of(2027, 12, 25, 3, 0)))));

        assertTrue(error.getMessage().contains("outside the period"));
    }

    @Test
    void rejectsAHeatThatStartsInsideThePeriodButEndsAfterIt() {
        TeamId delta = edition.registerEligibleTeam("Delta Bots");

        assertThrows(DomainException.class, () -> edition.scheduleRound(1,
                List.of(edition.heat(delta, "A1", TestEdition.LAST_DAY.atTime(23, 55)))));
    }

    @Test
    void rejectsTeamsThatWereNotAcceptedInTheCompetition() {
        TeamId rejected = edition.register("Rookies", TeamFixtures.membersWithUnderageCompetitor(),
                TeamFixtures.eligibleRobot(), TeamFixtures.completeDocuments()).teamId();

        DomainException error = assertThrows(DomainException.class,
                () -> edition.scheduleRound(1, List.of(edition.heat(rejected, "A1", TEN))));

        assertTrue(error.getMessage().contains("not accepted"));
    }
}
