package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.schedule.Round;
import com.dps.roboleague.domain.schedule.ScheduleConflictType;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.support.TeamFixtures;
import com.dps.roboleague.support.TestEdition;
import java.time.LocalDateTime;
import java.util.List;
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
