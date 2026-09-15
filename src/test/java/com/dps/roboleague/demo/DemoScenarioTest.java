package com.dps.roboleague.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.audit.AuditAction;
import com.dps.roboleague.domain.audit.AuditEvent;
import com.dps.roboleague.infrastructure.config.RoboLeagueModule;
import com.dps.roboleague.support.TestEdition;
import java.util.List;
import org.junit.jupiter.api.Test;

class DemoScenarioTest {

    @Test
    void theCompositionRootWiresEveryUseCaseOfTheEdition() {
        RoboLeagueModule module = RoboLeagueModule.inMemory(TestEdition.fixedClock());

        assertDoesNotThrow(() -> new DemoScenario(module).run());

        List<AuditAction> actions = module.auditLog().findBySubject("CATEGORY-1").stream()
                .map(AuditEvent::action)
                .toList();
        assertTrue(actions.containsAll(List.of(AuditAction.STANDINGS_GENERATED, AuditAction.STANDINGS_PUBLISHED,
                AuditAction.STANDINGS_RECALCULATED)));
    }
}
