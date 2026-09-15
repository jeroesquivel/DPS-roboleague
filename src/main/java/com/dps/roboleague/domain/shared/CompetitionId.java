package com.dps.roboleague.domain.shared;

public record CompetitionId(String value) implements Identifier {

    public CompetitionId {
        Identifier.validate(value, "CompetitionId");
    }

    public static CompetitionId of(String value) {
        return new CompetitionId(value);
    }
}
