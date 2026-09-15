package com.dps.roboleague.domain.eligibility.rule;

import com.dps.roboleague.domain.eligibility.EligibilityRequest;
import com.dps.roboleague.domain.eligibility.EligibilityRule;
import com.dps.roboleague.domain.eligibility.EligibilityViolation;
import com.dps.roboleague.domain.team.DocumentType;
import java.util.List;
import java.util.Set;

public final class RequiredDocumentsRule implements EligibilityRule {

    public static final String CODE = "REQUIRED_DOCUMENTS";

    private final Set<DocumentType> requiredDocuments;

    public RequiredDocumentsRule(Set<DocumentType> requiredDocuments) {
        this.requiredDocuments = Set.copyOf(requiredDocuments);
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public List<EligibilityViolation> evaluate(EligibilityRequest request) {
        Set<DocumentType> presented = request.registration().documentTypes();
        return requiredDocuments.stream()
                .filter(required -> !presented.contains(required))
                .map(missing -> new EligibilityViolation(CODE, "missing document " + missing))
                .toList();
    }
}
