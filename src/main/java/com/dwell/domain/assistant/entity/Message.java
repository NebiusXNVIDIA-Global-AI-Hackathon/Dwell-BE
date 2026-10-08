package com.dwell.domain.assistant.entity;

import com.dwell.domain.assistant.enums.MessageRole;
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

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // [{label, url}], required for legal answers
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> citations;

    // [{type, label, body, citation, dueAt}]
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Map<String, Object>> cards;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> quickReplies;

    // [fileId] (File purpose=CHAT_ATTACHMENT)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Long> attachments;

    @Builder
    public Message(Conversation conversation, MessageRole role, String content,
                   List<Map<String, Object>> citations, List<Map<String, Object>> cards,
                   List<String> quickReplies, List<Long> attachments) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.citations = citations;
        this.cards = cards;
        this.quickReplies = quickReplies;
        this.attachments = attachments;
    }
}
