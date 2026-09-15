package com.dps.roboleague;

import com.dps.roboleague.demo.DemoScenario;
import com.dps.roboleague.infrastructure.config.RoboLeagueModule;
import java.time.Clock;

public final class Main {

    public static void main(String[] args) {
        new DemoScenario(RoboLeagueModule.inMemory(Clock.systemUTC())).run();
    }
}
