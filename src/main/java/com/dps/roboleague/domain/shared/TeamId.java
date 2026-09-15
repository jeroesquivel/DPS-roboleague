package com.dps.roboleague.domain.shared;

public record TeamId(String value) implements Identifier {

    public TeamId {
        Identifier.validate(value, "TeamId");
    }

    public static TeamId of(String value) {
        return new TeamId(value);
    }
}
