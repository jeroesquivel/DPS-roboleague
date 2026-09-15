package com.dps.roboleague.domain.team;

import com.dps.roboleague.domain.shared.DomainException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

public record Member(String fullName, LocalDate birthDate, MemberRole role) {

    public Member {
        Objects.requireNonNull(birthDate, "birth date is required");
        Objects.requireNonNull(role, "member role is required");
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("member requires a full name");
        }
    }

    public int ageOn(LocalDate referenceDate) {
        if (referenceDate.isBefore(birthDate)) {
            throw new DomainException("reference date cannot be before the birth date of " + fullName);
        }
        return Period.between(birthDate, referenceDate).getYears();
    }

    public boolean isCompetitor() {
        return role == MemberRole.COMPETITOR;
    }
}
