package com.dps.roboleague.support;

import com.dps.roboleague.domain.competition.RobotClass;
import com.dps.roboleague.domain.team.Dimensions;
import com.dps.roboleague.domain.team.DocumentType;
import com.dps.roboleague.domain.team.Member;
import com.dps.roboleague.domain.team.MemberRole;
import com.dps.roboleague.domain.team.Robot;
import com.dps.roboleague.domain.team.TeamDocument;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TeamFixtures {

    public static final RobotClass RESCUE_BOT = RobotClass.of("RESCUE_BOT");

    private TeamFixtures() {
    }

    public static List<Member> eligibleMembers() {
        return List.of(
                new Member("Ada", LocalDate.of(2010, 5, 20), MemberRole.COMPETITOR),
                new Member("Linus", LocalDate.of(2011, 8, 3), MemberRole.COMPETITOR),
                new Member("Grace", LocalDate.of(1988, 2, 10), MemberRole.COACH));
    }

    public static List<Member> membersWithUnderageCompetitor() {
        return List.of(
                new Member("Ada", LocalDate.of(2010, 5, 20), MemberRole.COMPETITOR),
                new Member("Tim", LocalDate.of(2018, 1, 15), MemberRole.COMPETITOR),
                new Member("Grace", LocalDate.of(1988, 2, 10), MemberRole.COACH));
    }

    public static List<Member> membersWithoutCoach() {
        return List.of(
                new Member("Ada", LocalDate.of(2010, 5, 20), MemberRole.COMPETITOR),
                new Member("Linus", LocalDate.of(2011, 8, 3), MemberRole.COMPETITOR));
    }

    public static Robot eligibleRobot() {
        return new Robot("Rescuer", RESCUE_BOT, new BigDecimal("2.400"), new Dimensions(180, 180, 150));
    }

    public static Robot overweightRobot() {
        return new Robot("Heavy", RESCUE_BOT, new BigDecimal("4.100"), new Dimensions(180, 180, 150));
    }

    public static Robot wrongClassRobot() {
        return new Robot("Sumo", RobotClass.of("SUMO"), new BigDecimal("2.000"), new Dimensions(180, 180, 150));
    }

    public static List<TeamDocument> completeDocuments() {
        return List.of(new TeamDocument(DocumentType.PARENTAL_CONSENT, "PC-1"),
                new TeamDocument(DocumentType.TECHNICAL_SHEET, "TS-1"));
    }

    public static List<TeamDocument> incompleteDocuments() {
        return List.of(new TeamDocument(DocumentType.PARENTAL_CONSENT, "PC-1"));
    }
}
