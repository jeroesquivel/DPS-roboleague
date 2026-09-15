package com.dps.roboleague.domain.rulebook;

import com.dps.roboleague.domain.challenge.ChallengeSpec;
import com.dps.roboleague.domain.eligibility.EligibilityPolicy;
import com.dps.roboleague.domain.ranking.TiebreakRule;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DomainException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Rulebook(CompetitionId competitionId, RulebookVersion version, LocalDate publishedOn,
        Map<ChallengeId, ChallengeSpec> challenges, EligibilityPolicy eligibilityPolicy,
        List<TiebreakRule> tiebreakRules) {

    public Rulebook {
        Objects.requireNonNull(competitionId, "competition id is required");
        Objects.requireNonNull(version, "rulebook version is required");
        Objects.requireNonNull(publishedOn, "publication date is required");
        Objects.requireNonNull(eligibilityPolicy, "eligibility policy is required");
        challenges = Map.copyOf(challenges);
        tiebreakRules = List.copyOf(tiebreakRules);
        if (challenges.isEmpty()) {
            throw new DomainException("a rulebook requires at least one challenge");
        }
    }

    public static Builder builder(CompetitionId competitionId, RulebookVersion version, LocalDate publishedOn) {
        return new Builder(competitionId, version, publishedOn);
    }

    public ChallengeSpec challenge(ChallengeId challengeId) {
        ChallengeSpec spec = challenges.get(challengeId);
        if (spec == null) {
            throw new DomainException("challenge " + challengeId.value() + " is not defined in rulebook " + version);
        }
        return spec;
    }

    public static final class Builder {

        private final CompetitionId competitionId;
        private final RulebookVersion version;
        private final LocalDate publishedOn;
        private final Map<ChallengeId, ChallengeSpec> challenges = new LinkedHashMap<>();
        private final List<TiebreakRule> tiebreakRules = new ArrayList<>();
        private EligibilityPolicy eligibilityPolicy = EligibilityPolicy.of();

        private Builder(CompetitionId competitionId, RulebookVersion version, LocalDate publishedOn) {
            this.competitionId = competitionId;
            this.version = version;
            this.publishedOn = publishedOn;
        }

        public Builder withChallenge(ChallengeSpec spec) {
            challenges.put(spec.id(), spec);
            return this;
        }

        public Builder withEligibilityPolicy(EligibilityPolicy policy) {
            this.eligibilityPolicy = policy;
            return this;
        }

        public Builder withTiebreak(TiebreakRule rule) {
            tiebreakRules.add(rule);
            return this;
        }

        public Rulebook build() {
            return new Rulebook(competitionId, version, publishedOn, challenges, eligibilityPolicy, tiebreakRules);
        }
    }
}
