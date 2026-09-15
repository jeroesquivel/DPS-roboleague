package com.dps.roboleague.domain.shared;

public record RoundId(String value) implements Identifier {

    public RoundId {
        Identifier.validate(value, "RoundId");
    }

    public static RoundId of(String value) {
        return new RoundId(value);
    }
}
