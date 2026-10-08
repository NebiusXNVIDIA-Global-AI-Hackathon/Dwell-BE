package com.dwell.domain.building.entity;

import com.dwell.domain.building.enums.HpdRecordType;
import com.dwell.domain.building.enums.HpdStatus;
import com.dwell.global.common.enums.IssueType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// HPD complaints and violations stored in one table
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HpdViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HpdRecordType recordType;

    // Original HPD ID, unique together with record_type
    @Column(nullable = false, length = 20)
    private String hpdViolationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueType issueType;

    // Original HPD category value
    @Column(nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HpdStatus status;

    // A/B/C, only for VIOLATION
    @Column(length = 2)
    private String violationClass;

    @Column(nullable = false)
    private LocalDate reportedAt;

    private LocalDate closedAt;

    @Builder
    public HpdViolation(Building building, HpdRecordType recordType, String hpdViolationId, IssueType issueType,
                        String category, HpdStatus status, String violationClass,
                        LocalDate reportedAt, LocalDate closedAt) {
        this.building = building;
        this.recordType = recordType;
        this.hpdViolationId = hpdViolationId;
        this.issueType = issueType;
        this.category = category;
        this.status = status;
        this.violationClass = violationClass;
        this.reportedAt = reportedAt;
        this.closedAt = closedAt;
    }
}
