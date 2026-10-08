package com.dwell.domain.cases.entity;

import com.dwell.domain.cases.enums.ChecklistSource;
import com.dwell.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseChecklistItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    // English original for AI items, up to 100 chars for USER items
    @Column(nullable = false)
    private String text;

    // NULL for USER items
    @Column(columnDefinition = "TEXT")
    private String textNative;

    @Column(nullable = false)
    private boolean checked;

    // AI items cannot be edited
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChecklistSource source;

    @Column(nullable = false)
    private int sortOrder;

    @Builder
    public CaseChecklistItem(Case caseEntity, String text, String textNative, ChecklistSource source, int sortOrder) {
        this.caseEntity = caseEntity;
        this.text = text;
        this.textNative = textNative;
        this.source = source;
        this.sortOrder = sortOrder;
    }
}
