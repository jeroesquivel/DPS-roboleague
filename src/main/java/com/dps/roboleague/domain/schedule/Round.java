package com.dps.roboleague.domain.schedule;

import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.TeamId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Round {

    private final RoundId id;
    private final CompetitionId competitionId;
    private final CategoryId categoryId;
    private final ChallengeId challengeId;
    private final int ordinal;
    private final RulebookVersion rulebookVersion;
    private final List<Heat> heats = new ArrayList<>();

    public Round(RoundId id, CompetitionId competitionId, CategoryId categoryId, ChallengeId challengeId, int ordinal,
            RulebookVersion rulebookVersion) {
        this.id = Objects.requireNonNull(id, "round id is required");
        this.competitionId = Objects.requireNonNull(competitionId, "competition id is required");
        this.categoryId = Objects.requireNonNull(categoryId, "category id is required");
        this.challengeId = Objects.requireNonNull(challengeId, "challenge id is required");
        this.rulebookVersion = Objects.requireNonNull(rulebookVersion, "rulebook version is required");
        if (ordinal < 1) {
            throw new DomainException("a round ordinal must be positive");
        }
        this.ordinal = ordinal;
    }

    public void schedule(Heat heat) {
        if (!heat.roundId().equals(id)) {
            throw new DomainException("heat " + heat.id().value() + " does not belong to round " + id.value());
        }
        if (heatFor(heat.teamId()).isPresent()) {
            throw new DomainException("team " + heat.teamId().value() + " already has a heat in round " + id.value());
        }
        heats.add(heat);
    }

    public Optional<Heat> heatFor(TeamId teamId) {
        return heats.stream().filter(heat -> heat.teamId().equals(teamId)).findFirst();
    }

    public RoundId id() {
        return id;
    }

    public CompetitionId competitionId() {
        return competitionId;
    }

    public CategoryId categoryId() {
        return categoryId;
    }

    public ChallengeId challengeId() {
        return challengeId;
    }

    public int ordinal() {
        return ordinal;
    }

    public RulebookVersion rulebookVersion() {
        return rulebookVersion;
    }

    public List<Heat> heats() {
        return List.copyOf(heats);
    }
}
