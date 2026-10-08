package com.dwell.domain.cases.entity;

import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Can be regenerated, so a case may have several reports
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseReport extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    // First-person English incident summary
    @Column(nullable = false, columnDefinition = "TEXT")
    private String narrative;

    // Object Storage keys
    private String pdfKey;

    private String evidenceZipKey;

    @Builder
    public CaseReport(Case caseEntity, String narrative, String pdfKey, String evidenceZipKey) {
        this.caseEntity = caseEntity;
        this.narrative = narrative;
        this.pdfKey = pdfKey;
        this.evidenceZipKey = evidenceZipKey;
    }
}
