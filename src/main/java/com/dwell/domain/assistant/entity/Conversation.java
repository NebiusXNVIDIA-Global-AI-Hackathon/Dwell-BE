package com.dwell.domain.assistant.entity;

import com.dwell.domain.assistant.enums.ConversationType;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.user.entity.User;
import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ConversationType type;

    // Required when type is CASE, one conversation per case
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private Case caseEntity;

    @Builder
    public Conversation(User user, ConversationType type, Case caseEntity) {
        this.user = user;
        this.type = type;
        this.caseEntity = caseEntity;
    }
}
