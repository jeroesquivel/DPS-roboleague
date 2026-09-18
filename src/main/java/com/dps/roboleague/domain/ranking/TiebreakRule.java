package com.dps.roboleague.domain.ranking;

import java.util.Comparator;

public interface TiebreakRule extends Comparator<TeamScoreSummary> {

    String code();

    String description();
}
