package com.dps.roboleague.domain.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.TeamId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class StandingsTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-03-02T12:00:00Z");

    private final Standings provisional = Standings.provisional(CompetitionId.of("COMP-1"), CategoryId.of("CAT-1"),
            RulebookVersion.first(), GENERATED_AT, entries("TEAM-1", "TEAM-2"));

    @Test
    void isProvisionalUntilItIsPublished() {
        assertFalse(provisional.isFinal());
        assertEquals(1, provisional.revision());
    }

    @Test
    void becomesFinalOnlyOnce() {
        Standings published = provisional.publish();

        assertTrue(published.isFinal());
        assertThrows(DomainException.class, published::publish);
    }

    @Test
    void aRecalculationOpensANewProvisionalRevisionWithTheSameRulebook() {
        Standings recalculated = provisional.publish().supersede(entries("TEAM-2", "TEAM-1"),
                GENERATED_AT.plusSeconds(3600));

        assertEquals(2, recalculated.revision());
        assertEquals(PublicationStatus.PROVISIONAL, recalculated.status());
        assertEquals(RulebookVersion.first(), recalculated.rulebookVersion());
        assertEquals(1, recalculated.entryFor(TeamId.of("TEAM-2")).orElseThrow().position());
    }

    private List<StandingEntry> entries(String... teams) {
        return List.of(new StandingEntry(1, TeamId.of(teams[0]), Points.of(60), List.of()),
                new StandingEntry(2, TeamId.of(teams[1]), Points.of(50), List.of()));
    }
}
