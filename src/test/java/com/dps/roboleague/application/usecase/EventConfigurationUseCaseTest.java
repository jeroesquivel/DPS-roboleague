package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.in.CreateCompetition;
import com.dps.roboleague.application.port.in.CreateSeason;
import com.dps.roboleague.application.port.in.FindAuditTrail;
import com.dps.roboleague.application.port.in.FindCompetition;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.domain.competition.RobotClass;
import com.dps.roboleague.domain.shared.AgeRange;
import com.dps.roboleague.domain.shared.DateRange;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.SeasonId;
import com.dps.roboleague.infrastructure.config.RoboLeagueCompositionRoot;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventConfigurationUseCaseTest {

    private static final String ACTOR = "competition-organizer";
    private static final Instant NOW = Instant.parse("2026-02-01T12:00:00Z");
    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 3, 1);
    private static final LocalDate LAST_DAY = LocalDate.of(2026, 11, 30);
    private static final DateRange SEASON_PERIOD = DateRange.of(FIRST_DAY, LAST_DAY);
    private static final CreateCompetition.CategoryDraft JUNIOR = new CreateCompetition.CategoryDraft(
            "Junior", AgeRange.between(12, 17), RobotClass.of("RESCUE_BOT"));
    private static final CreateCompetition.CategoryDraft SENIOR = new CreateCompetition.CategoryDraft(
            "Senior", AgeRange.between(18, 30), RobotClass.of("SUMO"));

    private final RoboLeagueCompositionRoot module = RoboLeagueCompositionRoot.inMemory(
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsASeasonAndCompetitionWithDistinctCategoriesAndAnAuditablePublicView() {
        SeasonId seasonId = createSeason();
        DateRange competitionPeriod = DateRange.of(LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12));

        CreateCompetition.Result created = createCompetition(seasonId, competitionPeriod);
        FindCompetition.View competition = module.findCompetitionUseCase()
                .execute(created.competitionId());

        assertEquals("National Open", competition.name());
        assertEquals(competitionPeriod, competition.period());
        assertTrue(competition.activeRulebookVersion().isEmpty());
        assertEquals(2, created.categoryIds().size());
        assertNotEquals(created.categoryIds().get(0), created.categoryIds().get(1));
        assertEquals(List.of(
                new FindCompetition.CategoryView(created.categoryIds().get(0), JUNIOR.name(), JUNIOR.ageRange(),
                        JUNIOR.robotClass()),
                new FindCompetition.CategoryView(created.categoryIds().get(1), SENIOR.name(), SENIOR.ageRange(),
                        SENIOR.robotClass())), competition.categories());
        assertCreationAudited(seasonId.value(), AuditAction.SEASON_CREATED);
        assertCreationAudited(created.competitionId().value(), AuditAction.COMPETITION_CREATED);
    }

    @Test
    void acceptsACompetitionThatMatchesBothSeasonBoundaries() {
        CreateCompetition.Result created = createCompetition(createSeason(), SEASON_PERIOD);

        FindCompetition.View competition = module.findCompetitionUseCase()
                .execute(created.competitionId());

        assertEquals(SEASON_PERIOD, competition.period());
        assertCreationAudited(created.competitionId().value(), AuditAction.COMPETITION_CREATED);
    }

    @Test
    void acceptsASingleDayCompetitionOnTheLastDayOfTheSeason() {
        DateRange singleDay = DateRange.of(LAST_DAY, LAST_DAY);

        CreateCompetition.Result created = createCompetition(createSeason(), singleDay);
        FindCompetition.View competition = module.findCompetitionUseCase()
                .execute(created.competitionId());

        assertEquals(singleDay, competition.period());
        assertCreationAudited(created.competitionId().value(), AuditAction.COMPETITION_CREATED);
    }

    @Test
    void rejectsASeasonWhosePeriodDoesNotStartInItsDeclaredYear() {
        assertThrows(DomainException.class, () -> module.createSeasonUseCase()
                .execute(new CreateSeason.Command("Season 2025", 2025, SEASON_PERIOD, ACTOR)));
    }

    @Test
    void rejectsACompetitionStartingBeforeTheSeasonEvenWhenItsEndIsInside() {
        SeasonId seasonId = createSeason();
        DateRange outsideAtStart = DateRange.of(FIRST_DAY.minusDays(1), FIRST_DAY.plusDays(2));

        assertThrows(DomainException.class, () -> createCompetition(seasonId, outsideAtStart));
    }

    @Test
    void rejectsACompetitionEndingAfterTheSeasonEvenWhenItsStartIsInside() {
        SeasonId seasonId = createSeason();
        DateRange outsideAtEnd = DateRange.of(LAST_DAY.minusDays(2), LAST_DAY.plusDays(1));

        assertThrows(DomainException.class, () -> createCompetition(seasonId, outsideAtEnd));
    }

    @Test
    void rejectsACompetitionForAnUnknownSeason() {
        SeasonId missingSeason = SeasonId.of("missing-season");

        NotFoundException error = assertThrows(NotFoundException.class,
                () -> createCompetition(missingSeason, SEASON_PERIOD));

        assertTrue(error.getMessage().contains(missingSeason.value()));
    }

    private SeasonId createSeason() {
        return module.createSeasonUseCase()
                .execute(new CreateSeason.Command("Season 2026", 2026, SEASON_PERIOD, ACTOR));
    }

    private CreateCompetition.Result createCompetition(SeasonId seasonId, DateRange period) {
        return module.createCompetitionUseCase().execute(new CreateCompetition.Command(
                seasonId, "National Open", period, List.of(JUNIOR, SENIOR), ACTOR));
    }

    private void assertCreationAudited(String subject, AuditAction action) {
        List<AuditEvent> events = module.findAuditTrailUseCase().execute(new FindAuditTrail.Command(subject));
        assertEquals(1, events.size());
        AuditEvent event = events.getFirst();
        assertEquals(action, event.action());
        assertEquals(ACTOR, event.actor());
        assertEquals(NOW, event.occurredAt());
    }
}
