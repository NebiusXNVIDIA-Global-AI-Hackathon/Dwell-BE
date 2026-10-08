package com.dwell.domain.cases.entity;

import com.dwell.domain.cases.enums.CaseEventRefType;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.global.common.LocalizedText;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

// Case timeline entry
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseEventType type;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CaseEventRefType refType;

    // ID of the ref_type target (one row per evidence for EVIDENCE_ADDED)
    private Long refId;

    // Per type: {srNumber}, {violationNumber, violationClass}, {scheduledStart, scheduledEnd, repairedBy}, ...
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    // Highlighted as a warning on the timeline
    @Column(nullable = false)
    private boolean alert;

    @Builder
    public CaseEvent(Case caseEntity, CaseEventType type, LocalDateTime occurredAt, LocalizedText title,
                     CaseEventRefType refType, Long refId, Map<String, Object> payload, boolean alert) {
        this.caseEntity = caseEntity;
        this.type = type;
        this.occurredAt = occurredAt;
        this.title = title;
        this.refType = refType;
        this.refId = refId;
        this.payload = payload;
        this.alert = alert;
    }
}
