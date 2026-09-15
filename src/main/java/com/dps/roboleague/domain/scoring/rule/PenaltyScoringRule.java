package com.dps.roboleague.domain.scoring.rule;

import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.PenaltyCode;
import com.dps.roboleague.domain.scoring.PenaltyDefinition;
import com.dps.roboleague.domain.scoring.ScoreContribution;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.scoring.ScoringRule;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.Points;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PenaltyScoringRule implements ScoringRule {

    public static final String CODE = "PENALTIES";

    private final Map<PenaltyCode, PenaltyDefinition> catalog = new LinkedHashMap<>();

    public PenaltyScoringRule(Collection<PenaltyDefinition> definitions) {
        definitions.forEach(definition -> catalog.put(definition.code(), definition));
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<ScoreContribution> apply(ScoringContext context) {
        return context.incidents().stream().map(this::contributionFor).toList();
    }

    private ScoreContribution contributionFor(IncidentReport incident) {
        PenaltyDefinition definition = catalog.get(incident.code());
        if (definition == null) {
            throw new DomainException("penalty " + incident.code().value() + " is not defined in the rulebook");
        }
        Points deduction = definition.deduction().times(BigDecimal.valueOf(incident.occurrences())).negated();
        String explanation = "%s applied %d time(s)".formatted(definition.description(), incident.occurrences());
        return new ScoreContribution(CODE, explanation, deduction);
    }
}
