package com.dps.roboleague.application.port.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.ranking.PublicationStatus;
import com.dps.roboleague.domain.ranking.StandingEntry;
import com.dps.roboleague.domain.ranking.Standings;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.TeamId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class StandingsRepositoryContractTest {

    private static final CompetitionId COMPETITION = CompetitionId.of("COMP-1");
    private static final CategoryId CATEGORY = CategoryId.of("CAT-1");
    private static final Instant NOW = Instant.parse("2026-03-02T10:00:00Z");

    protected abstract StandingsRepository repository();

    @Test
    void findLatestIsEmptyWhenNothingWasGenerated() {
        assertTrue(repository().findLatest(COMPETITION, CATEGORY).isEmpty());
        assertTrue(repository().findHistory(COMPETITION, CATEGORY).isEmpty());
    }

    @Test
    void publishingReplacesTheProvisionalRevisionInsteadOfAddingOne() {
        StandingsRepository repository = repository();
        Standings provisional = provisional();
        repository.save(provisional);

        repository.save(provisional.publish());

        assertEquals(1, repository.findHistory(COMPETITION, CATEGORY).size());
        assertEquals(PublicationStatus.FINAL, repository.findLatest(COMPETITION, CATEGORY).orElseThrow().status());
    }

    @Test
    void aRecalculationKeepsTheRevisionsThatCameBefore() {
        StandingsRepository repository = repository();
        Standings published = provisional().publish();
        repository.save(published);

        repository.save(published.supersede(List.of(entry(1, "OMEGA")), NOW.plusSeconds(60)));

        List<Standings> history = repository.findHistory(COMPETITION, CATEGORY);
        assertEquals(List.of(1, 2), history.stream().map(Standings::revision).toList());
        assertEquals(PublicationStatus.FINAL, history.getFirst().status());
        assertEquals(2, repository.findLatest(COMPETITION, CATEGORY).orElseThrow().revision());
    }

    @Test
    void findHistoryReturnsTheRevisionsFromTheOldestToTheNewest() {
        StandingsRepository repository = repository();
        Standings first = provisional();
        Standings second = first.supersede(List.of(entry(1, "OMEGA")), NOW.plusSeconds(60));
        repository.save(second);
        repository.save(first);

        assertEquals(List.of(1, 2), repository.findHistory(COMPETITION, CATEGORY).stream()
                .map(Standings::revision).toList());
    }

    @Test
    void revisionsOfOneCategoryDoNotLeakIntoAnother() {
        StandingsRepository repository = repository();
        repository.save(provisional());

        assertTrue(repository.findLatest(COMPETITION, CategoryId.of("CAT-2")).isEmpty());
    }

    private Standings provisional() {
        return Standings.provisional(COMPETITION, CATEGORY, RulebookVersion.first(), NOW,
                List.of(entry(1, "DELTA")));
    }

    private StandingEntry entry(int position, String teamId) {
        return new StandingEntry(position, TeamId.of(teamId), Points.of(60), List.of());
    }
}
