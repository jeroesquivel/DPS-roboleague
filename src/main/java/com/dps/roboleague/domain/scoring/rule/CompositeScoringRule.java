package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.shared.DomainException;
import java.util.List;

public record CompositeScoringRule(List<ScoringRule> rules) implements ScoringRule {

    public CompositeScoringRule {
        rules = List.copyOf(rules);
        if (rules.isEmpty()) {
            throw new DomainException("a composite scoring rule requires at least one rule");
        }
    }

    public static CompositeScoringRule of(ScoringRule... rules) {
        return new CompositeScoringRule(List.of(rules));
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        return rules.stream().flatMap(rule -> rule.apply(context).stream()).toList();
    }
}
