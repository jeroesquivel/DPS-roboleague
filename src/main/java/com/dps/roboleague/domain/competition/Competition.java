package com.dps.roboleague.domain.competition;

import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DateRange;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.SeasonId;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Competition {

    private final CompetitionId id;
    private final SeasonId seasonId;
    private final String name;
    private final DateRange period;
    private final Map<CategoryId, Category> categories = new LinkedHashMap<>();
    private RulebookVersion activeRulebookVersion;

    public Competition(CompetitionId id, SeasonId seasonId, String name, DateRange period, Collection<Category> categories) {
        this.id = Objects.requireNonNull(id, "competition id is required");
        this.seasonId = Objects.requireNonNull(seasonId, "season id is required");
        this.period = Objects.requireNonNull(period, "competition period is required");
        if (name == null || name.isBlank()) {
            throw new DomainException("competition requires a name");
        }
        this.name = name;
        categories.forEach(this::addCategory);
    }

    public void addCategory(Category category) {
        Objects.requireNonNull(category, "category is required");
        if (categories.containsKey(category.id())) {
            throw new DomainException("category " + category.id().value() + " is already registered");
        }
        categories.put(category.id(), category);
    }

    public Category category(CategoryId categoryId) {
        Category category = categories.get(categoryId);
        if (category == null) {
            throw new DomainException("category " + categoryId.value() + " does not belong to competition " + name);
        }
        return category;
    }

    public void requireDateWithinPeriod(LocalDate date) {
        Objects.requireNonNull(date, "date is required");
        if (!period.contains(date)) {
            throw new DomainException(
                    "date " + date + " is outside the period of competition " + name);
        }
    }

    public void activateRulebook(RulebookVersion version) {
        Objects.requireNonNull(version, "rulebook version is required");
        if (activeRulebookVersion != null && !version.isNewerThan(activeRulebookVersion)) {
            throw new DomainException("rulebook version " + version + " does not supersede " + activeRulebookVersion);
        }
        this.activeRulebookVersion = version;
    }

    public RulebookVersion requireActiveRulebookVersion() {
        return activeRulebookVersion().orElseThrow(
                () -> new DomainException("competition " + name + " has no published rulebook"));
    }

    public Optional<RulebookVersion> activeRulebookVersion() {
        return Optional.ofNullable(activeRulebookVersion);
    }

    public CompetitionId id() {
        return id;
    }

    public SeasonId seasonId() {
        return seasonId;
    }

    public String name() {
        return name;
    }

    public DateRange period() {
        return period;
    }

    public List<Category> categories() {
        return List.copyOf(categories.values());
    }
}
