package com.dwell.domain.legal.entity;

import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Embedded chunk for Legal RAG (pgvector)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalChunk extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private LegalDocument document;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1024)
    @Column(nullable = false)
    private float[] embedding;

    @Builder
    public LegalChunk(LegalDocument document, String content, float[] embedding) {
        this.document = document;
        this.content = content;
        this.embedding = embedding;
    }
}
