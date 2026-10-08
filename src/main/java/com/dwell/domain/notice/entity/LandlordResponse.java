package com.dwell.domain.notice.entity;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.notice.enums.ResponseIntent;
import com.dwell.global.common.BaseCreatedTimeEntity;
import com.dwell.global.common.LocalizedText;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Landlord reply pasted by the user and analyzed by AI
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LandlordResponse extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(length = 100)
    private String senderName;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResponseIntent intent;

    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText headline;

    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText explanation;

    @Column(columnDefinition = "TEXT")
    private String translationNative;

    private LocalDate repairDate;

    @Column(length = 20)
    private String repairTime;

    @Column(length = 100)
    private String repairedBy;

    private LocalDateTime confirmedAt;

    @Builder
    public LandlordResponse(Case caseEntity, String rawText, String senderName, LocalDateTime receivedAt) {
        this.caseEntity = caseEntity;
        this.rawText = rawText;
        this.senderName = senderName;
        this.receivedAt = receivedAt;
    }
}
