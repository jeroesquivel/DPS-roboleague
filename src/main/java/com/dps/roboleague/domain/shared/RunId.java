package com.dps.roboleague.domain.shared;

public record RunId(String value) implements Identifier {

    public RunId {
        Identifier.validate(value, "RunId");
    }

    public static RunId of(String value) {
        return new RunId(value);
    }
}
