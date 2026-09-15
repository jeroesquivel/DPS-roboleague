package com.dps.roboleague.domain.shared;

public record ChallengeId(String value) implements Identifier {

    public ChallengeId {
        Identifier.validate(value, "ChallengeId");
    }

    public static ChallengeId of(String value) {
        return new ChallengeId(value);
    }
}
