package com.dps.roboleague.domain.team;

import com.dps.roboleague.domain.shared.CategoryId;
import com.dps.roboleague.domain.shared.CompetitionId;
import com.dps.roboleague.domain.shared.DomainException;
import com.dps.roboleague.domain.shared.TeamId;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TeamRegistration {

    private final TeamId id;
    private final CompetitionId competitionId;
    private final CategoryId categoryId;
    private final String name;
    private final List<Member> members;
    private final Robot robot;
    private final Map<DocumentType, TeamDocument> documents = new EnumMap<>(DocumentType.class);
    private RegistrationStatus status = RegistrationStatus.SUBMITTED;
    private List<String> rejectionReasons = List.of();

    public TeamRegistration(TeamId id, CompetitionId competitionId, CategoryId categoryId, String name,
            Collection<Member> members, Robot robot, Collection<TeamDocument> documents) {
        this.id = Objects.requireNonNull(id, "team id is required");
        this.competitionId = Objects.requireNonNull(competitionId, "competition id is required");
        this.categoryId = Objects.requireNonNull(categoryId, "category id is required");
        this.robot = Objects.requireNonNull(robot, "robot is required");
        if (name == null || name.isBlank()) {
            throw new DomainException("team requires a name");
        }
        if (members == null || members.isEmpty()) {
            throw new DomainException("team " + name + " requires at least one member");
        }
        this.name = name;
        this.members = List.copyOf(members);
        documents.forEach(document -> this.documents.put(document.type(), document));
    }

    public void accept() {
        requireDecidable();
        this.status = RegistrationStatus.ACCEPTED;
        this.rejectionReasons = List.of();
    }

    public void reject(List<String> reasons) {
        requireDecidable();
        if (reasons.isEmpty()) {
            throw new DomainException("a rejection requires at least one reason");
        }
        this.status = RegistrationStatus.REJECTED;
        this.rejectionReasons = List.copyOf(reasons);
    }

    private void requireDecidable() {
        if (status != RegistrationStatus.SUBMITTED) {
            throw new DomainException(
                    "registration of team " + name + " was already resolved as " + status);
        }
    }

    public boolean isAccepted() {
        return status == RegistrationStatus.ACCEPTED;
    }

    public List<Member> competitors() {
        return members.stream().filter(Member::isCompetitor).toList();
    }

    public Set<DocumentType> documentTypes() {
        return Set.copyOf(documents.keySet());
    }

    public TeamId id() {
        return id;
    }

    public CompetitionId competitionId() {
        return competitionId;
    }

    public CategoryId categoryId() {
        return categoryId;
    }

    public String name() {
        return name;
    }

    public List<Member> members() {
        return members;
    }

    public Robot robot() {
        return robot;
    }

    public RegistrationStatus status() {
        return status;
    }

    public List<String> rejectionReasons() {
        return rejectionReasons;
    }
}
