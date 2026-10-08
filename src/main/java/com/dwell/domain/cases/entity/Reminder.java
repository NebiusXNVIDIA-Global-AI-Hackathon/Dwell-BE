package com.dwell.domain.cases.entity;

import com.dwell.domain.cases.enums.ReminderType;
import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReminderType type;

    @Column(nullable = false)
    private LocalDateTime fireAt;

    private LocalDateTime firedAt;

    private LocalDateTime canceledAt;

    @Builder
    public Reminder(Case caseEntity, ReminderType type, LocalDateTime fireAt) {
        this.caseEntity = caseEntity;
        this.type = type;
        this.fireAt = fireAt;
    }
}
