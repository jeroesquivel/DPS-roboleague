package com.dps.roboleague.application.service;

import com.dps.roboleague.application.NotFoundException;
import com.dps.roboleague.application.port.out.RoundRepository;
import com.dps.roboleague.application.port.out.RulebookRepository;
import com.dps.roboleague.application.port.out.RunResultRepository;
import com.dps.roboleague.domain.ranking.ScoredRun;
import com.dps.roboleague.domain.ranking.TeamScoreSummary;
import com.dps.roboleague.domain.result.RunResult;
import com.dps.roboleague.domain.rulebook.Rulebook;
import com.dps.roboleague.domain.schedule.Round;
import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.TeamId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scores every captured run of a category with the rulebook version pinned to that run, so a
 * recalculation always reproduces the figures of the edition the result belongs to.
 */
public final class CategoryScoreCollector {

    private final RoundRepository rounds;
    private final RunResultRepository runResults;
    private final RulebookRepository rulebooks;

    public CategoryScoreCollector(RoundRepository rounds, RunResultRepository runResults,
            RulebookRepository rulebooks) {
        this.rounds = rounds;
        this.runResults = runResults;
        this.rulebooks = rulebooks;
    }

    public List<TeamScoreSummary> collect(CompetitionId competitionId, CategoryId categoryId) {
        Map<TeamId, List<ScoredRun>> runsByTeam = new LinkedHashMap<>();
        for (Round round : rounds.findByCategory(competitionId, categoryId)) {
            for (RunResult run : runResults.findByRound(round.id())) {
                runsByTeam.computeIfAbsent(run.teamId(), team -> new ArrayList<>())
                        .add(scoreRun(run, competitionId));
            }
        }
        return runsByTeam.entrySet().stream()
                .map(entry -> new TeamScoreSummary(entry.getKey(), entry.getValue()))
                .toList();
    }

    public ScoredRun scoreRun(RunResult run, CompetitionId competitionId) {
        Rulebook rulebook = rulebooks.find(competitionId, run.rulebookVersion())
                .orElseThrow(() -> NotFoundException.of("Rulebook", run.rulebookVersion().toString()));
        return new ScoredRun(run.id(), run.challengeId(), run.currentMeasurements(),
                rulebook.challenge(run.challengeId()).score(run.scoringContext()));
    }
}
