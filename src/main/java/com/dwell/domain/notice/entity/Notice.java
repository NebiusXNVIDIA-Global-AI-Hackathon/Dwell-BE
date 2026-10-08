package com.dwell.domain.notice.entity;

import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.notice.enums.NoticeChannel;
import com.dwell.domain.notice.enums.NoticeType;
import com.dwell.global.common.BaseCreatedTimeEntity;
import com.dwell.global.common.LocalizedText;
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

// Formal notice to the landlord
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NoticeType type;

    // Cites NYC regulations
    @Column(nullable = false, columnDefinition = "TEXT")
    private String bodyEn;

    // Back-translation into the user's language
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mirrorNative;

    // [{label, url}]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> citations;

    // RPL §223-b
    @JdbcTypeCode(SqlTypes.JSON)
    private LocalizedText retaliationNotice;

    // Saved at send time, including the user's edits
    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private NoticeChannel channel;

    // SLA starts here
    private LocalDateTime sentAt;

    // Urgent 24h / hazardous 72h / normal 7 days
    private LocalDateTime slaDueAt;

    @Builder
    public Notice(Case caseEntity, NoticeType type, String bodyEn, String mirrorNative,
                  List<Map<String, Object>> citations, LocalizedText retaliationNotice) {
        this.caseEntity = caseEntity;
        this.type = type;
        this.bodyEn = bodyEn;
        this.mirrorNative = mirrorNative;
        this.citations = citations;
        this.retaliationNotice = retaliationNotice;
    }
}
