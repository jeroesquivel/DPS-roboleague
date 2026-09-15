package com.dps.roboleague.domain.ranking;

import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.TeamId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Standings(CompetitionId competitionId, CategoryId categoryId, RulebookVersion rulebookVersion,
        int revision, PublicationStatus status, Instant generatedAt, List<StandingEntry> entries) {

    public Standings {
        Objects.requireNonNull(competitionId, "competition id is required");
        Objects.requireNonNull(categoryId, "category id is required");
        Objects.requireNonNull(rulebookVersion, "rulebook version is required");
        Objects.requireNonNull(status, "publication status is required");
        Objects.requireNonNull(generatedAt, "generation timestamp is required");
        if (revision < 1) {
            throw new DomainException("a standings revision must be positive");
        }
        entries = List.copyOf(entries);
    }

    public static Standings provisional(CompetitionId competitionId, CategoryId categoryId, RulebookVersion version,
            Instant generatedAt, List<StandingEntry> entries) {
        return new Standings(competitionId, categoryId, version, 1, PublicationStatus.PROVISIONAL, generatedAt,
                entries);
    }

    public Standings publish() {
        if (status == PublicationStatus.FINAL) {
            throw new DomainException("standings revision " + revision + " is already final");
        }
        return new Standings(competitionId, categoryId, rulebookVersion, revision, PublicationStatus.FINAL,
                generatedAt, entries);
    }

    public Standings supersede(List<StandingEntry> newEntries, Instant recalculatedAt) {
        return new Standings(competitionId, categoryId, rulebookVersion, revision + 1, PublicationStatus.PROVISIONAL,
                recalculatedAt, newEntries);
    }

    public boolean isFinal() {
        return status == PublicationStatus.FINAL;
    }

    public Optional<StandingEntry> entryFor(TeamId teamId) {
        return entries.stream().filter(entry -> entry.teamId().equals(teamId)).findFirst();
    }
}
