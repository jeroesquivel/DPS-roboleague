package com.dps.roboleague.domain.team;

import com.dps.roboleague.domain.shared.DomainException;

public record Dimensions(int lengthMm, int widthMm, int heightMm) {

    public Dimensions {
        if (lengthMm <= 0 || widthMm <= 0 || heightMm <= 0) {
            throw new DomainException("dimensions must be positive");
        }
    }

    public boolean fitsWithin(Dimensions limit) {
        return lengthMm <= limit.lengthMm && widthMm <= limit.widthMm && heightMm <= limit.heightMm;
    }
}
