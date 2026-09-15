package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.shared.DomainException;
import java.util.List;

public record CompositeScoringRule(String code, List<ScoringRule> rules) implements ScoringRule {

    public CompositeScoringRule {
        if (code == null || code.isBlank()) {
            throw new DomainException("a composite scoring rule requires a code");
        }
        rules = List.copyOf(rules);
        if (rules.isEmpty()) {
            throw new DomainException("a composite scoring rule requires at least one rule");
        }
    }

    public static CompositeScoringRule of(String code, ScoringRule... rules) {
        return new CompositeScoringRule(code, List.of(rules));
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        return rules.stream().flatMap(rule -> rule.apply(context).stream()).toList();
    }
}
