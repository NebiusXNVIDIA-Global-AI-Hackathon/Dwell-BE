package com.dwell.domain.evidence.entity;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.evidence.enums.RepairVerdict;
import com.dwell.global.common.BaseCreatedTimeEntity;
import com.dwell.global.common.LocalizedText;
import com.dwell.global.common.enums.AnalysisStatus;
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

// Before/after evidence comparison
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RepairVerification extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "before_evidence_id", nullable = false)
    private Evidence beforeEvidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "after_evidence_id", nullable = false)
    private Evidence afterEvidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AnalysisStatus analysisStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RepairVerdict verdict;

    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText headline;

    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText summary;

    // [{label, result(PASS/FAIL/UNKNOWN), detail}]
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> checks;

    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText disclaimer;

    private LocalDateTime confirmedAt;

    @Builder
    public RepairVerification(Case caseEntity, Evidence beforeEvidence, Evidence afterEvidence,
                              AnalysisStatus analysisStatus) {
        this.caseEntity = caseEntity;
        this.beforeEvidence = beforeEvidence;
        this.afterEvidence = afterEvidence;
        this.analysisStatus = analysisStatus;
    }
}
