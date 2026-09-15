package com.dps.roboleague.domain.ranking.rule;

import com.dps.roboleague.domain.ranking.TeamScoreSummary;
import com.dps.roboleague.domain.ranking.TiebreakRule;

public final class FewestPenaltiesTiebreak implements TiebreakRule {

    public static final String CODE = "FEWEST_PENALTIES";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String description() {
        return "the team with the smallest penalty deduction ranks first";
    }

    @Override
    public int compare(TeamScoreSummary left, TeamScoreSummary right) {
        return right.penaltyPoints().compareTo(left.penaltyPoints());
    }
}
