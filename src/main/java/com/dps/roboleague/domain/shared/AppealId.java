package com.dps.roboleague.domain.shared;

public record AppealId(String value) implements Identifier {

    public AppealId {
        Identifier.validate(value, "AppealId");
    }

    public static AppealId of(String value) {
        return new AppealId(value);
    }
}
