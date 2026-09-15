package com.dps.roboleague.domain.shared;

public record CategoryId(String value) implements Identifier {

    public CategoryId {
        Identifier.validate(value, "CategoryId");
    }

    public static CategoryId of(String value) {
        return new CategoryId(value);
    }
}
