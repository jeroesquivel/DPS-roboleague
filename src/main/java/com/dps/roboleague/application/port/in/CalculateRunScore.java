package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.scoring.ScoreBreakdown;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.Points;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;

public interface CalculateRunScore {

    RunScore execute(Command command);

    record Command(RunId runId) {
    }

    record RunScore(RunId runId, TeamId teamId, ChallengeId challengeId, RulebookVersion rulebookVersion,
            ScoreBreakdown breakdown) {

        public Points total() {
            return breakdown.total();
        }
    }
}
