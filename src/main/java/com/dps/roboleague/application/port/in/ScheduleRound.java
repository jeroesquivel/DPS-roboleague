package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.schedule.TimeSlot;
import com.dps.roboleague.domain.shared.ArenaId;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.JudgeId;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.TeamId;
import java.util.List;
import java.util.Set;

public interface ScheduleRound {

    RoundId execute(Command command);

    record Command(CompetitionId competitionId, CategoryId categoryId, ChallengeId challengeId, int ordinal,
            List<HeatDraft> heats, String actor) {
    }

    record HeatDraft(TeamId teamId, ArenaId arenaId, TimeSlot slot, Set<JudgeId> judges) {
    }
}
