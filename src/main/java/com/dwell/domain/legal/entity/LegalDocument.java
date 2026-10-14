package com.dwell.domain.legal.entity;

import com.dwell.domain.legal.enums.Jurisdiction;
import com.dwell.domain.legal.enums.LegalTextType;
import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalDocument extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Corpus id the LLM cites, e.g. NYC-HMC-27-2005
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    // e.g. NYC HMC §27-2005
    @Column(nullable = false, unique = true, length = 100)
    private String label;

    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Jurisdiction jurisdiction;

    // e.g. ["leak", "mold"] or ["all"]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> issueTypes;

    // e.g. ["notice", "assistant"]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> usedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LegalTextType textType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Builder
    public LegalDocument(String code, String label, String title, String url, Jurisdiction jurisdiction,
                         List<String> issueTypes, List<String> usedBy, LegalTextType textType, String summary) {
        this.code = code;
        this.label = label;
        this.title = title;
        this.url = url;
        this.jurisdiction = jurisdiction;
        this.issueTypes = issueTypes;
        this.usedBy = usedBy;
        this.textType = textType;
        this.summary = summary;
    }

    // Re-sync from the corpus file (code stays the same)
    public void update(String label, String title, String url, Jurisdiction jurisdiction,
                       List<String> issueTypes, List<String> usedBy, LegalTextType textType, String summary) {
        this.label = label;
        this.title = title;
        this.url = url;
        this.jurisdiction = jurisdiction;
        this.issueTypes = issueTypes;
        this.usedBy = usedBy;
        this.textType = textType;
        this.summary = summary;
    }
}
