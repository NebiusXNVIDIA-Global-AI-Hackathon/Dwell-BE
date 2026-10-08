package com.dwell.domain.building.entity;

import com.dwell.domain.building.enums.SignalConfidence;
import com.dwell.domain.building.enums.SignalScope;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.IssueCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Issue pattern detected in a building (API signalId)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuildingSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueCategory issueCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SignalScope scope;

    // e.g. Floors 3-5
    private Integer floorFrom;

    private Integer floorTo;

    // Public app cases + HPD records
    @Column(nullable = false)
    private int reportCount;

    // e.g. Last 30 days
    @Column(nullable = false)
    private int windowDays;

    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText insight;

    // [{key, label{en,native}, referenceImageUrl}]
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> guidedInspection;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private SignalConfidence confidence;

    // {by_floor, by_issue}, anonymous aggregates only
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> breakdown;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    @Builder
    public BuildingSignal(Building building, IssueCategory issueCategory, SignalScope scope, Integer floorFrom,
                          Integer floorTo, int reportCount, int windowDays, LocalizedText insight,
                          List<Map<String, Object>> guidedInspection, SignalConfidence confidence,
                          Map<String, Object> breakdown, LocalDateTime computedAt) {
        this.building = building;
        this.issueCategory = issueCategory;
        this.scope = scope;
        this.floorFrom = floorFrom;
        this.floorTo = floorTo;
        this.reportCount = reportCount;
        this.windowDays = windowDays;
        this.insight = insight;
        this.guidedInspection = guidedInspection;
        this.confidence = confidence;
        this.breakdown = breakdown;
        this.computedAt = computedAt;
    }
}
