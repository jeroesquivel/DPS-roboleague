package com.dps.roboleague.domain.rulebook;

import com.dps.roboleague.domain.shared.DomainException;

public record RulebookVersion(int number) implements Comparable<RulebookVersion> {

    public RulebookVersion {
        if (number < 1) {
            throw new DomainException("rulebook version must be positive");
        }
    }

    public static RulebookVersion first() {
        return new RulebookVersion(1);
    }

    public static RulebookVersion of(int number) {
        return new RulebookVersion(number);
    }

    public RulebookVersion next() {
        return new RulebookVersion(number + 1);
    }

    public boolean isNewerThan(RulebookVersion other) {
        return number > other.number;
    }

    @Override
    public int compareTo(RulebookVersion other) {
        return Integer.compare(number, other.number);
    }

    @Override
    public String toString() {
        return "v" + number;
    }
}
