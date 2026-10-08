package com.dwell.domain.building.entity;

import com.dwell.domain.building.enums.InspectionOutcome;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.user.entity.User;
import com.dwell.global.common.BaseCreatedTimeEntity;
import com.dwell.global.common.enums.IssueCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

// Result of a guided self-inspection for a building signal
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InspectionResult extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    // Set when "I found an issue" leads to a new case
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private Case caseEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_id")
    private BuildingSignal signal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueCategory issueCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InspectionOutcome outcome;

    // e.g. ["RADIATOR_COLD", "ROOM_BELOW_68F"]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> symptoms;

    @Builder
    public InspectionResult(User user, Building building, Case caseEntity, BuildingSignal signal,
                            IssueCategory issueCategory, InspectionOutcome outcome, List<String> symptoms) {
        this.user = user;
        this.building = building;
        this.caseEntity = caseEntity;
        this.signal = signal;
        this.issueCategory = issueCategory;
        this.outcome = outcome;
        this.symptoms = symptoms;
    }
}
