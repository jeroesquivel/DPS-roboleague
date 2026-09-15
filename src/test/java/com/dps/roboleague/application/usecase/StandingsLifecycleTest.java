package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.port.in.GenerateStandings;
import com.dps.roboleague.application.port.in.PublishStandings;
import com.dps.roboleague.support.RescueEditionFixture;
import com.dps.roboleague.domain.ranking.StandingEntry;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.support.TestEdition;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StandingsLifecycleTest {

    private final TestEdition edition = TestEdition.start();
    private final TeamId delta = edition.registerEligibleTeam("Delta Bots");
    private final TeamId omega = edition.registerEligibleTeam("Omega Crew");

    @BeforeEach
    void captureTheRoundResults() {
        RoundId roundId = edition.scheduleRoundFor(1, List.of(delta, omega));
        edition.capture(roundId, delta, "95.5", 4, "42", List.of(8, 9), List.of());
        edition.capture(roundId, omega, "105", 5, "55", List.of(7, 7),
                List.of(IncidentReport.once(RescueEditionFixture.RESTART)));
    }

    @Test
    void generatesProvisionalStandingsOrderedByTotalPoints() {
        Standings standings = generate();

        assertFalse(standings.isFinal());
        assertEquals(1, standings.revision());
        assertEquals(List.of(omega, delta), standings.entries().stream().map(StandingEntry::teamId).toList());
        assertEquals(Points.of("71.50"), standings.entryFor(omega).orElseThrow().totalPoints());
        assertEquals(Points.of("60.75"), standings.entryFor(delta).orElseThrow().totalPoints());
    }

    @Test
    void publishingTurnsTheStandingsIntoTheFinalResult() {
        generate();

        Standings published = edition.module().publishStandingsUseCase()
                .execute(new PublishStandings.Command(edition.competitionId(), edition.categoryId(),
                        TestEdition.ACTOR));

        assertTrue(published.isFinal());
        assertTrue(edition.latestStandings().isFinal());
    }

    @Test
    void refusesToGenerateTwiceSoThatCorrectionsGoThroughARecalculation() {
        generate();

        DomainException error = assertThrows(DomainException.class, this::generate);

        assertTrue(error.getMessage().contains("recalculation"));
    }

    private Standings generate() {
        return edition.module().generateStandingsUseCase()
                .execute(new GenerateStandings.Command(edition.competitionId(), edition.categoryId(),
                        TestEdition.ACTOR));
    }
}
