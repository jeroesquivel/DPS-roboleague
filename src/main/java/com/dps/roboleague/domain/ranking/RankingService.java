package com.dps.roboleague.domain.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RankingService {

    public List<StandingEntry> rank(List<TeamScoreSummary> summaries, List<TiebreakRule> tiebreakRules) {
        Comparator<TeamScoreSummary> ranking = rankingComparator(tiebreakRules);
        List<TeamScoreSummary> sorted = summaries.stream()
                .sorted(ranking.thenComparing(summary -> summary.teamId().value()))
                .toList();

        List<StandingEntry> entries = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            TeamScoreSummary current = sorted.get(index);
            TeamScoreSummary previous = index == 0 ? null : sorted.get(index - 1);
            boolean sharesPosition = previous != null && ranking.compare(previous, current) == 0;
            int position = sharesPosition ? entries.get(index - 1).position() : index + 1;
            entries.add(new StandingEntry(position, current.teamId(), current.totalPoints(),
                    appliedTiebreaks(previous, current, tiebreakRules)));
        }
        return List.copyOf(entries);
    }

    private Comparator<TeamScoreSummary> rankingComparator(List<TiebreakRule> tiebreakRules) {
        Comparator<TeamScoreSummary> comparator = Comparator.comparing(TeamScoreSummary::totalPoints).reversed();
        for (TiebreakRule rule : tiebreakRules) {
            comparator = comparator.thenComparing(rule);
        }
        return comparator;
    }

    private List<String> appliedTiebreaks(TeamScoreSummary previous, TeamScoreSummary current,
            List<TiebreakRule> tiebreakRules) {
        if (previous == null || previous.totalPoints().compareTo(current.totalPoints()) != 0) {
            return List.of();
        }
        return tiebreakRules.stream()
                .filter(rule -> rule.compare(previous, current) != 0)
                .findFirst()
                .map(rule -> List.of(rule.code()))
                .orElse(List.of());
    }
}
