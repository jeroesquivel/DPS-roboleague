package com.dps.roboleague.application.port.in;

import com.dps.roboleague.domain.challenge.ChallengeSpec;
import com.dps.roboleague.domain.eligibility.EligibilityPolicy;
import com.dps.roboleague.domain.ranking.TiebreakRule;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.shared.CompetitionId;
import java.util.List;

public interface PublishRulebook {

    RulebookVersion execute(Command command);

    record Command(CompetitionId competitionId, List<ChallengeSpec> challenges, EligibilityPolicy eligibilityPolicy,
            List<TiebreakRule> tiebreakRules, String actor) {
    }
}
