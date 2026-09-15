package com.dps.roboleague.domain.challenge;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.scoring.rule.ObjectiveScoringRule;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChallengeSpecTest {

    private static final MetricKey TIME = MetricKey.of("TIME");
    private static final MetricKey OBJECTIVES = MetricKey.of("OBJECTIVES");
    private static final MetricKey DESIGN = MetricKey.of("DESIGN");

    private final ChallengeSpec challenge = new ChallengeSpec(ChallengeId.of("RESCUE"), "Rescue mission",
            List.of(MetricDefinition.required(TIME, MetricKind.TIME_SECONDS, "s"),
                    MetricDefinition.required(OBJECTIVES, MetricKind.OBJECTIVE_COUNT, "objectives"),
                    MetricDefinition.optional(DESIGN, MetricKind.JUDGE_CRITERION, "points")),
            new ObjectiveScoringRule(OBJECTIVES, Points.of(10), 5), 2);

    @Test
    void acceptsAMeasurementSetThatCoversEveryRequiredMetric() {
        assertDoesNotThrow(() -> challenge.validate(complete()));
    }

    @Test
    void rejectsAMeasurementSetWithoutARequiredMetric() {
        MeasurementSet incomplete = MeasurementSet.empty().with(TIME, MetricValue.of("95.5"));

        DomainException error = assertThrows(DomainException.class, () -> challenge.validate(incomplete));

        assertTrue(error.getMessage().contains("OBJECTIVES"));
    }

    @Test
    void rejectsMetricsThatTheChallengeDoesNotDefine() {
        MeasurementSet unexpected = complete().with(MetricKey.of("BATTERY"), MetricValue.of("10"));

        DomainException error = assertThrows(DomainException.class, () -> challenge.validate(unexpected));

        assertTrue(error.getMessage().contains("BATTERY"));
    }

    @Test
    void rejectsValuesThatDoNotMatchTheKindOfTheMetric() {
        MeasurementSet fractionalObjectives = complete().with(OBJECTIVES, MetricValue.of("3.5"));

        assertThrows(DomainException.class, () -> challenge.validate(fractionalObjectives));
    }

    @Test
    void rejectsAttemptsBeyondTheConfiguredLimit() {
        assertDoesNotThrow(() -> challenge.requireAttemptWithinLimit(2));
        assertThrows(DomainException.class, () -> challenge.requireAttemptWithinLimit(3));
    }

    private MeasurementSet complete() {
        return MeasurementSet.empty()
                .with(TIME, MetricValue.of("95.5"))
                .with(OBJECTIVES, MetricValue.of(4));
    }
}
