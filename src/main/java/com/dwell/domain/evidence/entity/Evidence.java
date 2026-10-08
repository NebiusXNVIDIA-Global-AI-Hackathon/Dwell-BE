package com.dwell.domain.evidence.entity;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.evidence.enums.EvidencePhase;
import com.dwell.domain.evidence.enums.Progression;
import com.dwell.domain.user.entity.User;
import com.dwell.global.common.BaseCreatedTimeEntity;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.AnalysisStatus;
import com.dwell.global.common.enums.IssueType;
import com.dwell.global.common.enums.Location;
import com.dwell.global.common.enums.MediaType;
import com.dwell.global.common.enums.Severity;
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

// Evidence is only registered inside a case
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Evidence extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    // Owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // e.g. EV-001, unique per user
    @Column(nullable = false, length = 10)
    private String evidenceCode;

    // Decided from the attached files
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaType mediaType;

    // AFTER_REPAIR starts a repair verification
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvidencePhase phase = EvidencePhase.INITIAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Location location;

    // Required when location is OTHER
    @Column(length = 100)
    private String locationDetail;

    // Estimated by AI
    @Column(length = 20)
    private String subLocation;

    // Native language allowed
    @Column(length = 500)
    private String description;

    @Column(length = 10)
    private String descriptionLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AnalysisStatus analysisStatus;

    // Problem name decided by AI (Ceiling leak, No heat, ...)
    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Severity severity;

    // [{en, native}]
    @JdbcTypeCode(SqlTypes.JSON)
    private List<LocalizedText> reasons;

    // [{label, passed}]
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> qualityChecks;

    // Needs two or more points in time at the same location
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Progression progression;

    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText progressionReason;

    private LocalDateTime analyzedAt;

    // Soft delete
    private LocalDateTime deletedAt;

    @Builder
    public Evidence(Case caseEntity, User user, String evidenceCode, MediaType mediaType, EvidencePhase phase,
                    Location location, String locationDetail, String description, String descriptionLanguage,
                    AnalysisStatus analysisStatus) {
        this.caseEntity = caseEntity;
        this.user = user;
        this.evidenceCode = evidenceCode;
        this.mediaType = mediaType;
        if (phase != null) {
            this.phase = phase;
        }
        this.location = location;
        this.locationDetail = locationDetail;
        this.description = description;
        this.descriptionLanguage = descriptionLanguage;
        this.analysisStatus = analysisStatus;
    }
}
