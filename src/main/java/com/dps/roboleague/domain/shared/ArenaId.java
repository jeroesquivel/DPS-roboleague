package com.dps.roboleague.domain.shared;

public record ArenaId(String value) implements Identifier {

    public ArenaId {
        Identifier.validate(value, "ArenaId");
    }

    public static ArenaId of(String value) {
        return new ArenaId(value);
    }
}
