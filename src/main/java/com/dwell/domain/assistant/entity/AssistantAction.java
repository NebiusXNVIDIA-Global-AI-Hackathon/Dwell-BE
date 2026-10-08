package com.dwell.domain.assistant.entity;

import com.dwell.domain.assistant.enums.AssistantActionType;
import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

// Choice buttons attached to an assistant message (one per message)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssistantAction extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssistantActionType type;

    // [{key, label}]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<Map<String, Object>> choices;

    // Already chosen -> 409
    @Column(length = 20)
    private String chosen;

    // intent, extracted schedule, draftBody, prefill, ...
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Builder
    public AssistantAction(Message message, AssistantActionType type, List<Map<String, Object>> choices,
                           Map<String, Object> payload) {
        this.message = message;
        this.type = type;
        this.choices = choices;
        this.payload = payload;
    }
}
