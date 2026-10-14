package com.dwell.domain.legal.dto;

import com.dwell.domain.legal.entity.LegalDocument;

// 프롬프트에 넣고 인용으로 보여줄 법률 근거 (code = 모델이 인용하는 id)
public record LegalSource(String code, String label, String title, String url, String summary) {

    public static LegalSource from(LegalDocument document) {
        return new LegalSource(document.getCode(), document.getLabel(), document.getTitle(), document.getUrl(), document.getSummary());
    }
}
