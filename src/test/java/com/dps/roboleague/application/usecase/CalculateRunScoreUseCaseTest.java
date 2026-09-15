package com.dps.roboleague.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.port.in.CalculateRunScore;
import com.dps.roboleague.support.RescueEditionFixture;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.scoring.ScoreBreakdown;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.rule.ObjectiveScoringRule;
import com.dps.roboleague.domain.scoring.rule.TimeScoringRule;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;
import com.dps.roboleague.support.TestEdition;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculateRunScoreUseCaseTest {

    private final TestEdition edition = TestEdition.start();
    private final TeamId delta = edition.registerEligibleTeam("Delta Bots");
    private final RoundId firstRound = edition.scheduleRoundFor(1, List.of(delta));

    @Test
    void explainsEveryContributionThatBuildsTheTotal() {
        RunId runId = edition.capture(firstRound, delta, "95.5", 4, "42", List.of(8, 9), List.of());

        CalculateRunScore.RunScore score = score(runId);
        ScoreBreakdown breakdown = score.breakdown();

        assertEquals(Points.of("60.75"), score.total());
        assertEquals(Points.of("12.25"), breakdown.totalFor(TimeScoringRule.CODE));
        assertEquals(Points.of("40.00"), breakdown.totalFor(ObjectiveScoringRule.CODE));
        assertEquals(score.total(), breakdown.contributions().stream()
                .map(ScoreContribution::points).reduce(Points.ZERO, Points::plus));
        assertTrue(breakdown.contributions().stream().noneMatch(contribution -> contribution.explanation().isBlank()));
    }

    @Test
    void keepsUsingTheRulebookVersionPinnedWhenTheResultWasCaptured() {
        RunId runUnderFirstRulebook = edition.capture(firstRound, delta, "95.5", 4, "42", List.of(8, 9), List.of());

        edition.publishRulebookWith(new ObjectiveScoringRule(RescueEditionFixture.OBJECTIVES, Points.of(100), 5));
        RoundId secondRound = edition.scheduleRound(2,
                List.of(edition.heat(delta, "A2", LocalDateTime.of(2026, 3, 2, 12, 0))));
        RunId runUnderSecondRulebook = edition.capture(secondRound, delta, "95.5", 4, "42", List.of(8, 9), List.of());

        assertEquals(RulebookVersion.of(1), score(runUnderFirstRulebook).rulebookVersion());
        assertEquals(Points.of("60.75"), score(runUnderFirstRulebook).total());
        assertEquals(RulebookVersion.of(2), score(runUnderSecondRulebook).rulebookVersion());
        assertEquals(Points.of(400), score(runUnderSecondRulebook).total());
    }

    private CalculateRunScore.RunScore score(RunId runId) {
        return edition.module().calculateRunScoreUseCase().execute(new CalculateRunScore.Command(runId));
    }
}
