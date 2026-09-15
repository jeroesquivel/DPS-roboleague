package com.dps.roboleague.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.application.port.in.FindAuditTrail;
import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.infrastructure.config.RoboLeagueCompositionRoot;
import com.dps.roboleague.support.TestEdition;
import java.util.List;
import org.junit.jupiter.api.Test;

class DemoScenarioTest {

    @Test
    void theCompositionRootWiresEveryUseCaseOfTheEdition() {
        RoboLeagueCompositionRoot module = RoboLeagueCompositionRoot.inMemory(TestEdition.fixedClock());

        assertDoesNotThrow(() -> new DemoScenario(module).run());

        List<AuditAction> actions = FindAuditTrail.actionsOf(
                module.findAuditTrailUseCase().execute(new FindAuditTrail.Command("CATEGORY-1")));
        assertTrue(actions.containsAll(List.of(AuditAction.STANDINGS_GENERATED, AuditAction.STANDINGS_PUBLISHED,
                AuditAction.STANDINGS_RECALCULATED)));
    }
}
