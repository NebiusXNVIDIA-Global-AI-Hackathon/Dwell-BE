package com.dwell.domain.legal.entity;

import com.dwell.domain.legal.enums.Jurisdiction;
import com.dwell.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalDocument extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. NYC HMC §27-2005
    @Column(nullable = false, unique = true, length = 100)
    private String label;

    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Jurisdiction jurisdiction;

    @Builder
    public LegalDocument(String label, String title, String url, Jurisdiction jurisdiction) {
        this.label = label;
        this.title = title;
        this.url = url;
        this.jurisdiction = jurisdiction;
    }
}
