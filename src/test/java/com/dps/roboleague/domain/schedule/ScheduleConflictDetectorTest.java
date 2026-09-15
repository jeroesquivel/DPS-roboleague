package com.dps.roboleague.domain.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dps.roboleague.domain.shared.ArenaId;
import com.dps.roboleague.domain.shared.HeatId;
import com.dps.roboleague.domain.shared.JudgeId;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.TeamId;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScheduleConflictDetectorTest {

    private static final LocalDateTime TEN = LocalDateTime.of(2026, 3, 2, 10, 0);
    private static final RoundId ROUND = RoundId.of("ROUND-1");

    private final ScheduleConflictDetector detector = new ScheduleConflictDetector();
    private final Heat scheduled = heat("HEAT-1", "TEAM-1", "A1", TEN, Set.of(JudgeId.of("J1")));

    @Test
    void acceptsAHeatThatDoesNotOverlapWithTheScheduledOnes() {
        Heat candidate = heat("HEAT-2", "TEAM-1", "A1", TEN.plusMinutes(20), Set.of(JudgeId.of("J1")));

        assertTrue(detector.detect(List.of(scheduled), candidate).isEmpty());
    }

    @Test
    void detectsThatTheArenaIsAlreadyTaken() {
        Heat candidate = heat("HEAT-2", "TEAM-2", "A1", TEN.plusMinutes(5), Set.of(JudgeId.of("J2")));

        assertEquals(List.of(ScheduleConflictDetector.ARENA_BUSY), codesOf(candidate));
    }

    @Test
    void detectsThatTheTeamIsAlreadyRunning() {
        Heat candidate = heat("HEAT-2", "TEAM-1", "A2", TEN.plusMinutes(5), Set.of(JudgeId.of("J2")));

        assertEquals(List.of(ScheduleConflictDetector.TEAM_BUSY), codesOf(candidate));
    }

    @Test
    void detectsThatAJudgeIsAssignedToAnotherHeatAtTheSameTime() {
        Heat candidate = heat("HEAT-2", "TEAM-2", "A2", TEN.plusMinutes(5), Set.of(JudgeId.of("J1")));

        assertEquals(List.of(ScheduleConflictDetector.JUDGE_BUSY), codesOf(candidate));
    }

    private List<String> codesOf(Heat candidate) {
        return detector.detect(List.of(scheduled), candidate).stream().map(ScheduleConflict::code).toList();
    }

    private Heat heat(String id, String team, String arena, LocalDateTime start, Set<JudgeId> judges) {
        return new Heat(HeatId.of(id), ROUND, TeamId.of(team), ArenaId.of(arena),
                new TimeSlot(start, Duration.ofMinutes(15)), judges);
    }
}
