package com.dps.roboleague.domain.shared;

public interface Identifier {

    String value();

    static void validate(String value, String type) {
        if (value == null || value.isBlank()) {
            throw new DomainException(type + " requires a non blank value");
        }
    }
}
