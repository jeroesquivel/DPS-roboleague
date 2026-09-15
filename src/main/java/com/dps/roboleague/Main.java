package com.dps.roboleague;

import com.dps.roboleague.demo.DemoScenario;
import com.dps.roboleague.infrastructure.config.RoboLeagueCompositionRoot;
import java.time.Clock;

public final class Main {

    public static void main(String[] args) {
        new DemoScenario(RoboLeagueCompositionRoot.inMemory(Clock.systemUTC())).run();
    }
}
