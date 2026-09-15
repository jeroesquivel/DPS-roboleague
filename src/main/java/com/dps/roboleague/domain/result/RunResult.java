package com.dps.roboleague.domain.result;

import com.dps.roboleague.domain.challenge.MeasurementSet;
import com.dps.roboleague.domain.rulebook.RulebookVersion;
import com.dps.roboleague.domain.scoring.IncidentReport;
import com.dps.roboleague.domain.scoring.JudgeEvaluation;
import com.dps.roboleague.domain.scoring.ScoringContext;
import com.dps.roboleague.domain.shared.ChallengeId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.HeatId;
import com.dps.roboleague.domain.shared.RoundId;
import com.dps.roboleague.domain.shared.RunId;
import com.dps.roboleague.domain.shared.TeamId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RunResult {

    private final RunId id;
    private final RoundId roundId;
    private final HeatId heatId;
    private final TeamId teamId;
    private final ChallengeId challengeId;
    private final RulebookVersion rulebookVersion;
    private final int attemptNumber;
    private final Instant capturedAt;
    private final MeasurementSet originalMeasurements;
    private final List<IncidentReport> originalIncidents;
    private final List<JudgeEvaluation> evaluations;
    private final List<ResultCorrection> corrections = new ArrayList<>();

    public RunResult(RunId id, RoundId roundId, HeatId heatId, TeamId teamId, ChallengeId challengeId,
            RulebookVersion rulebookVersion, int attemptNumber, Instant capturedAt, MeasurementSet measurements,
            Collection<JudgeEvaluation> evaluations, Collection<IncidentReport> incidents) {
        this.id = Objects.requireNonNull(id, "run id is required");
        this.roundId = Objects.requireNonNull(roundId, "round id is required");
        this.heatId = Objects.requireNonNull(heatId, "heat id is required");
        this.teamId = Objects.requireNonNull(teamId, "team id is required");
        this.challengeId = Objects.requireNonNull(challengeId, "challenge id is required");
        this.rulebookVersion = Objects.requireNonNull(rulebookVersion, "rulebook version is required");
        this.capturedAt = Objects.requireNonNull(capturedAt, "capture timestamp is required");
        this.originalMeasurements = Objects.requireNonNull(measurements, "measurements are required");
        if (attemptNumber < 1) {
            throw new DomainException("an attempt number must be positive");
        }
        this.attemptNumber = attemptNumber;
        this.evaluations = List.copyOf(evaluations);
        this.originalIncidents = List.copyOf(incidents);
    }

    public void applyCorrection(ResultCorrection correction) {
        Objects.requireNonNull(correction, "correction is required");
        if (correction.appliedAt().isBefore(capturedAt)) {
            throw new DomainException("a correction cannot predate the capture of run " + id.value());
        }
        corrections.add(correction);
    }

    public MeasurementSet currentMeasurements() {
        return lastCorrection().map(ResultCorrection::measurements).orElse(originalMeasurements);
    }

    public List<IncidentReport> currentIncidents() {
        return lastCorrection().map(ResultCorrection::incidents).orElse(originalIncidents);
    }

    public ScoringContext scoringContext() {
        return new ScoringContext(currentMeasurements(), evaluations, currentIncidents());
    }

    public RunStatus status() {
        return corrections.isEmpty() ? RunStatus.CAPTURED : RunStatus.CORRECTED;
    }

    private Optional<ResultCorrection> lastCorrection() {
        return corrections.isEmpty() ? Optional.empty() : Optional.of(corrections.getLast());
    }

    public RunId id() {
        return id;
    }

    public RoundId roundId() {
        return roundId;
    }

    public HeatId heatId() {
        return heatId;
    }

    public TeamId teamId() {
        return teamId;
    }

    public ChallengeId challengeId() {
        return challengeId;
    }

    public RulebookVersion rulebookVersion() {
        return rulebookVersion;
    }

    public int attemptNumber() {
        return attemptNumber;
    }

    public Instant capturedAt() {
        return capturedAt;
    }

    public MeasurementSet originalMeasurements() {
        return originalMeasurements;
    }

    public List<IncidentReport> originalIncidents() {
        return originalIncidents;
    }

    public List<JudgeEvaluation> evaluations() {
        return evaluations;
    }

    public List<ResultCorrection> corrections() {
        return List.copyOf(corrections);
    }
}
