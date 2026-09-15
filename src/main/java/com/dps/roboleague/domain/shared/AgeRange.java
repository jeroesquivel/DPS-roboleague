package com.dps.roboleague.domain.shared;

public record AgeRange(int minimumYears, int maximumYears) {

    public AgeRange {
        if (minimumYears < 0) {
            throw new DomainException("minimum age cannot be negative");
        }
        if (maximumYears < minimumYears) {
            throw new DomainException("maximum age cannot be lower than minimum age");
        }
    }

    public static AgeRange between(int minimumYears, int maximumYears) {
        return new AgeRange(minimumYears, maximumYears);
    }

    public boolean includes(int years) {
        return years >= minimumYears && years <= maximumYears;
    }
}
